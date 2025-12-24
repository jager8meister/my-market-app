package ru.yandex.practicum.mymarket.controllers;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.service.CartService;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Cart API")
public class ApiCartController {

	private final CartService cartService;

	@GetMapping("/items")
	@Operation(summary = "Get cart state")
	public Mono<CartStateResponseDto> getCart(WebSession session) {
		return cartService.getCart(session);
	}

	@PostMapping("/items")
	@Operation(summary = "Update cart with action")
	public Mono<CartStateResponseDto> updateCart(@Valid @ModelAttribute CartUpdateRequestDto request, WebSession session) {
		return cartService.updateCart(request, session);
	}
}
