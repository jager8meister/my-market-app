package ru.yandex.practicum.mymarket.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.WebSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.exception.PaymentException;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.client.PaymentClient;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.UserService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartService cartService;
	private final OrderMapper orderMapper;
	private final TransactionalOperator transactionalOperator;
	private final PaymentClient paymentClient;
	private final UserService userService;

	@Override
	public Mono<OrderResponseDto> buy(WebSession session) {
		log.info("buy called - creating order from cart");
		return userService.getCurrentUserId()
				.zipWith(cartService.getItems(session).collectList())
				.doOnNext(tuple -> log.debug("Cart contains {} items for user {}", tuple.getT2().size(), tuple.getT1()))
				.flatMap(tuple -> {
					Long userId = tuple.getT1();
					List<CartEntry> cartEntries = tuple.getT2();

					return calculateTotalReactive(cartEntries)
							.flatMap(totalSum -> {
								log.debug("Checking balance for user {}: required amount = {}", userId, totalSum);
								return userService.hasEnoughBalance(userId, totalSum)
										.flatMap(hasEnough -> {
											if (!hasEnough) {
												log.warn("Insufficient balance for user {}", userId);
												return Mono.error(new ru.yandex.practicum.mymarket.exception.InsufficientBalanceException(
														"Недостаточно средств для оформления заказа"));
											}
											log.info("Balance check passed. Creating order for user {}, amount: {}", userId, totalSum);
											return createOrderFromCart(userId, cartEntries)
													.as(transactionalOperator::transactional);
										});
							});
				})
				.flatMap(order -> {
					log.info("Order created with id: {}, status: {}, total: {}. Creating payment...",
							order.getId(), order.getStatus(), order.getTotalSum());
					return createPaymentForOrder(order)
							.flatMap(paidOrder -> {
								log.info("Payment successful for order {}. Updating status to PAID", paidOrder.getId());
								paidOrder.setStatus(ru.yandex.practicum.mymarket.entity.OrderStatus.PAID);
								paidOrder.setUpdatedAt(LocalDateTime.now());
								return orderRepository.save(paidOrder)
										.as(transactionalOperator::transactional);
							})
							.onErrorResume(error -> {
								log.error("Payment failed for order {}: {}. Updating status to FAILED",
										order.getId(), error.getMessage());
								order.setStatus(ru.yandex.practicum.mymarket.entity.OrderStatus.FAILED);
								order.setUpdatedAt(LocalDateTime.now());
								return orderRepository.save(order)
										.as(transactionalOperator::transactional)
										.then(Mono.error(error));
							});
				})
				.flatMap(order -> buildOrderResponse(order))
				.flatMap(response -> {
					log.info("Order {} successfully paid. Clearing cart", response.id());
					return cartService.clear(session).thenReturn(response);
				})
				.doOnSuccess(order -> log.info("Order {} completed successfully with status PAID", order.id()))
				.doOnError(error -> log.error("Failed to complete order: {}", error.getMessage()));
	}

	@Override
	@Transactional(readOnly = true)
	public Flux<OrderResponseDto> getOrders() {
		log.debug("getOrders called");
		return userService.getCurrentUserId()
				.flatMapMany(userId -> {
					log.debug("Getting orders for user {}", userId);
					return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
				})
				.doOnNext(order -> log.debug("Found order: id={}, total={}", order.getId(), order.getTotalSum()))
				.flatMap(this::buildOrderResponse)
				.doOnComplete(() -> log.debug("getOrders completed"));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<OrderResponseDto> getOrder(long id) {
		log.debug("getOrder called with id: {}", id);
		return userService.getCurrentUserId()
				.flatMap(currentUserId -> orderRepository.findById(id)
						.switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with id: " + id)))
						.doOnError(error -> log.warn("Order not found with id: {}", id))
						.flatMap(order -> {
							if (!order.getUserId().equals(currentUserId)) {
								log.warn("User {} attempted to access order {} belonging to user {}",
										currentUserId, id, order.getUserId());
								return Mono.error(new ru.yandex.practicum.mymarket.exception.AccessDeniedException(
										"Access denied to order " + id));
							}
							return buildOrderResponse(order);
						}))
				.doOnSuccess(order -> log.debug("getOrder returned order: id={}, items count: {}",
						order.id(), order.items().size()));
	}

	private Mono<OrderEntity> createOrderFromCart(Long userId, List<CartEntry> cartEntries) {
		if (cartEntries == null || cartEntries.isEmpty()) {
			log.warn("Attempt to create order from empty cart");
			return Mono.error(new EmptyCartException("Cannot create order from empty cart"));
		}

		log.debug("Creating order from {} cart entries for user {}", cartEntries.size(), userId);
		return calculateTotalReactive(cartEntries)
				.doOnNext(total -> log.debug("Calculated order total: {}", total))
				.flatMap(totalSum -> {
					OrderEntity order = new OrderEntity(null, userId, totalSum, LocalDateTime.now());
					return orderRepository.save(order)
							.doOnSuccess(saved -> log.debug("Order entity saved with id: {}", saved.getId()))
							.flatMap(saved -> saveOrderItems(saved.getId(), cartEntries)
									.thenReturn(saved));
				});
	}

	private Mono<Long> calculateTotalReactive(List<CartEntry> cartEntries) {
		return Flux.fromIterable(cartEntries)
				.filter(entry -> entry.getCount() > 0)
				.map(entry -> entry.getItem().getPrice() * entry.getCount())
				.reduce(0L, Long::sum);
	}

	private Mono<Void> saveOrderItems(Long orderId, List<CartEntry> cartEntries) {
		log.debug("Saving order items for order id: {}", orderId);
		return Flux.fromIterable(cartEntries)
				.filter(entry -> entry.getCount() > 0)
				.doOnNext(entry -> log.debug("Saving order item: orderId={}, itemTitle={}, count={}",
						orderId, entry.getItem().getTitle(), entry.getCount()))
				.map(entry -> toOrderItem(orderId, entry))
				.flatMap(orderItemRepository::save)
				.then()
				.doOnSuccess(v -> log.debug("All order items saved for order id: {}", orderId));
	}

	private OrderItemEntity toOrderItem(Long orderId, CartEntry entry) {
		return new OrderItemEntity(
				null,
				orderId,
				entry.getItem().getTitle(),
				entry.getItem().getPrice(),
				entry.getCount()
		);
	}

	private Mono<OrderResponseDto> buildOrderResponse(OrderEntity orderEntity) {
		return orderItemRepository.findByOrderId(orderEntity.getId())
				.map(orderMapper::toOrderItemResponse)
				.collectList()
				.map(items -> new OrderResponseDto(
						orderEntity.getId(),
						items,
						orderEntity.getTotalSum(),
						orderEntity.getCreatedAt()
				));
	}

	private Mono<OrderEntity> createPaymentForOrder(OrderEntity order) {
		String description = "Оплата заказа #" + order.getId();
		log.info("Creating payment for order {}, amount: {}, user: {}", order.getId(), order.getTotalSum(), order.getUserId());

		return paymentClient.createPayment(order.getId(), order.getUserId(), order.getTotalSum(), description)
				.doOnSuccess(payment -> {
					log.info("Payment {} created for order {} with status {}",
							payment.getId(), order.getId(), payment.getStatus());
					order.setPaymentId(payment.getId().toString());
				})
				.onErrorMap(error -> {
					log.error("Failed to create payment for order {}: {}", order.getId(), error.getMessage());
					return new PaymentException("Не удалось создать платеж для заказа #" + order.getId());
				})
				.thenReturn(order);
	}
}
