package ru.yandex.practicum.mymarket.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Cart page and order creation")
public class CartController {

	private final CartService cartService;
	private final OrderService orderService;

	@GetMapping("/cart/items")
	@Operation(summary = "Show cart page")
	public String showCart(Model model) {
		return cartService.showCart(model);
	}

	@PostMapping("/cart/items")
	@Operation(summary = "Change item count on cart page")
	public String updateCart(@Valid CartUpdateRequestDto request) {
		return cartService.updateCart(request);
	}

	@PostMapping("/buy")
	@Operation(summary = "Create order from cart")
	public String buy() {
		return orderService.buy();
	}
}
