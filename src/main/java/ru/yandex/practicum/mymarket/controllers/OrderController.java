package ru.yandex.practicum.mymarket.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.mymarket.dto.request.OrderDetailsRequestDto;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Orders list and order details pages")
public class OrderController {

	private final OrderService orderService;

	@GetMapping("/orders")
	@Operation(summary = "Show orders list page with pagination")
	public String showOrders(@PageableDefault(size = 10) Pageable pageable, Model model) {
		return orderService.showOrders(pageable, model);
	}

	@GetMapping("/orders/{id}")
	@Operation(summary = "Show order details page")
	public String showOrder(
			@Parameter(description = "Order identifier", required = true)
			@PathVariable("id") long id,
			@Parameter(description = "Flag that order has just been created", example = "false")
			@RequestParam(name = "newOrder", required = false, defaultValue = "false") boolean newOrder,
			Model model) {
		OrderDetailsRequestDto request = new OrderDetailsRequestDto();
		request.setId(id);
		request.setNewOrder(newOrder);
		return orderService.showOrder(request, model);
	}
}
