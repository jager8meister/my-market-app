package ru.yandex.practicum.mymarket.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.dto.request.OrderDetailsRequestDto;
import ru.yandex.practicum.mymarket.service.model.CartEntry;
import ru.yandex.practicum.mymarket.service.model.OrderModel;

public interface OrderService {

	OrderModel createOrderFromCart(List<CartEntry> cartEntries);

	Page<OrderModel> getOrders(Pageable pageable);

	Optional<OrderModel> getOrder(long id);

	String buy();

	String showOrders(Pageable pageable, Model model);

	String showOrder(OrderDetailsRequestDto request, Model model);
}
