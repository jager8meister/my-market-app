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
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartService cartService;
	private final OrderMapper orderMapper;
	private final TransactionalOperator transactionalOperator;

	@Override
	public Mono<OrderResponseDto> buy(WebSession session) {
		log.info("buy called - creating order from cart");
		return cartService.getItems(session).collectList()
				.flatMap(this::createOrderFromCart)
				.as(transactionalOperator::transactional)
				.flatMap(this::buildOrderResponse)
				.flatMap(response -> cartService.clear(session).thenReturn(response));
	}

	@Override
	@Transactional(readOnly = true)
	public Flux<OrderResponseDto> getOrders() {
		return orderRepository.findAllByOrderByCreatedAtDesc()
				.flatMap(this::buildOrderResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<OrderResponseDto> getOrder(long id) {
		return orderRepository.findById(id)
				.switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with id: " + id)))
				.flatMap(this::buildOrderResponse);
	}

	private Mono<OrderEntity> createOrderFromCart(List<CartEntry> cartEntries) {
		if (cartEntries == null || cartEntries.isEmpty()) {
			log.warn("Attempt to create order from empty cart");
			return Mono.error(new EmptyCartException("Cannot create order from empty cart"));
		}

		OrderEntity order = new OrderEntity(null, calculateTotal(cartEntries), LocalDateTime.now());
		return orderRepository.save(order)
				.flatMap(saved -> saveOrderItems(saved.getId(), cartEntries).thenReturn(saved));
	}

	private long calculateTotal(List<CartEntry> cartEntries) {
		return cartEntries.stream()
				.filter(entry -> entry.getCount() > 0)
				.mapToLong(entry -> entry.getItem().getPrice() * entry.getCount())
				.sum();
	}

	private Mono<Void> saveOrderItems(Long orderId, List<CartEntry> cartEntries) {
		return Flux.fromIterable(cartEntries)
				.filter(entry -> entry.getCount() > 0)
				.flatMap(entry -> orderItemRepository.save(toOrderItem(orderId, entry)))
				.then();
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
}
