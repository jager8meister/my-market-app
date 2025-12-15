package ru.yandex.practicum.mymarket.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.mymarket.dto.request.OrderDetailsRequestDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;
import ru.yandex.practicum.mymarket.service.model.OrderItemModel;
import ru.yandex.practicum.mymarket.service.model.OrderModel;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final CartService cartService;
	private final OrderMapper orderMapper;

	@Override
	@Transactional
	public OrderModel createOrderFromCart(List<CartEntry> cartEntries) {
		if (cartEntries == null || cartEntries.isEmpty()) {
			log.warn("Attempt to create order from empty cart");
			throw new EmptyCartException("Cannot create order from empty cart");
		}

		log.info("Creating order from cart with {} items", cartEntries.size());
		OrderEntity orderEntity = new OrderEntity();
		orderEntity.setCreatedAt(LocalDateTime.now());

		long total = 0;

		for (CartEntry entry : cartEntries) {
			int count = entry.getCount();
			if (count <= 0) {
				continue;
			}
			long price = entry.getItem().getPrice();
			long itemTotal = price * count;
			total += itemTotal;

			OrderItemEntity itemEntity = new OrderItemEntity(
					entry.getItem().getTitle(),
					price,
					count
			);
			orderEntity.addItem(itemEntity);
		}

		orderEntity.setTotalSum(total);
		OrderEntity savedOrder = orderRepository.save(orderEntity);
		log.info("Order created successfully with id: {}, total: {}", savedOrder.getId(), total);

		return orderMapper.toOrderModel(savedOrder);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<OrderModel> getOrders(Pageable pageable) {
		log.debug("getOrders called with pageable: {}", pageable);
		return orderRepository.findAllWithItems(pageable)
				.map(orderMapper::toOrderModel);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<OrderModel> getOrder(long id) {
		log.debug("getOrder called with id: {}", id);
		return orderRepository.findByIdWithItems(id)
				.map(orderMapper::toOrderModel);
	}

	@Override
	public String buy() {
		log.info("buy called - creating order from cart");
		List<CartEntry> cartEntries = cartService.getItems();
		if (cartEntries.isEmpty()) {
			throw new EmptyCartException("Cannot create order from empty cart");
		}
		OrderModel order = createOrderFromCart(cartEntries);
		cartService.clear();
		return "redirect:/orders/" + order.getId() + "?newOrder=true";
	}

	@Override
	public String showOrders(Pageable pageable, Model model) {
		log.debug("showOrders called with pageable: {}", pageable);
		Page<OrderResponseDto> ordersPage = getOrders(pageable)
				.map(orderMapper::toOrderResponse);

		model.addAttribute("orders", ordersPage.getContent());
		model.addAttribute("pageNumber", ordersPage.getNumber() + 1);
		model.addAttribute("pageSize", ordersPage.getSize());
		model.addAttribute("totalPages", ordersPage.getTotalPages());
		model.addAttribute("hasNext", ordersPage.hasNext());
		model.addAttribute("hasPrevious", ordersPage.hasPrevious());

		return "orders";
	}

	@Override
	public String showOrder(OrderDetailsRequestDto request, Model model) {
		log.debug("showOrder called with request: {}", request);
		OrderModel order = getOrder(request.getId())
				.orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + request.getId()));
		List<OrderItemModel> items = order.getItems();

		model.addAttribute("order", orderMapper.toOrderResponse(order));
		model.addAttribute("items", items.stream().map(orderMapper::toOrderItemResponse).toList());
		model.addAttribute("newOrder", request.isNewOrder());

		return "order";
	}
}
