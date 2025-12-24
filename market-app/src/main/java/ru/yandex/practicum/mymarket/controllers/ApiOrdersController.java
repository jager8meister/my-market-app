package ru.yandex.practicum.mymarket.controllers;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.exception.ServiceUnavailableException;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentServiceHealthCheck;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
@Tag(name = "Orders", description = "Orders API")
public class ApiOrdersController {

	private final OrderService orderService;
	private final PaymentServiceHealthCheck paymentServiceHealthCheck;

	@PostMapping("/buy")
	@Operation(summary = "Create order from cart (buy)")
	public Mono<OrderResponseDto> createOrder(WebSession session) {
		if (!paymentServiceHealthCheck.isPaymentServiceAvailable()) {
			return Mono.error(new ServiceUnavailableException(
					"Payment service is currently unavailable. Please try again later."));
		}
		return orderService.buy(session);
	}

	@GetMapping("/orders")
	@Operation(summary = "List orders")
	public Flux<OrderResponseDto> getOrders() {
		return orderService.getOrders();
	}

	@GetMapping("/orders/{id}")
	@Operation(summary = "Get order by id")
	public Mono<OrderResponseDto> getOrder(@PathVariable("id") @Positive long id) {
		return orderService.getOrder(id);
	}
}
