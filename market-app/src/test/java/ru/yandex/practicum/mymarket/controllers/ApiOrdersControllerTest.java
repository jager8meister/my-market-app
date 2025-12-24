package ru.yandex.practicum.mymarket.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.service.OrderService;

class ApiOrdersControllerTest {

	private StubOrderService orderService;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		orderService = new StubOrderService();
		ApiOrdersController controller = new ApiOrdersController(orderService);
		webTestClient = WebTestClient.bindToController(controller).build();
	}

	@Test
	void buy_createsOrder() {
		OrderResponseDto response = new OrderResponseDto(1L, List.of(), 500L, LocalDateTime.now());
		orderService.buyResponse = Mono.just(response);

		webTestClient.post()
				.uri("/api/buy")
				.exchange()
				.expectStatus().isOk()
				.expectBody(OrderResponseDto.class)
				.isEqualTo(response);
	}

	@Test
	void getOrders_returnsList() {
		OrderItemResponseDto item = new OrderItemResponseDto("Title", 200L, 2);
		OrderResponseDto response = new OrderResponseDto(2L, List.of(item), 400L, LocalDateTime.now());
		orderService.ordersFlux = Flux.just(response);

		webTestClient.get()
				.uri("/api/orders")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(OrderResponseDto.class)
				.hasSize(1)
				.contains(response);
	}

	@Test
	void getOrder_returnsSingle() {
		OrderResponseDto response = new OrderResponseDto(3L, List.of(), 0L, LocalDateTime.now());
		orderService.orderResponse = Mono.just(response);

		webTestClient.get()
				.uri("/api/orders/3")
				.exchange()
				.expectStatus().isOk()
				.expectBody(OrderResponseDto.class)
				.isEqualTo(response);
	}

	private static class StubOrderService implements OrderService {
		private Mono<OrderResponseDto> buyResponse = Mono.empty();
		private Flux<OrderResponseDto> ordersFlux = Flux.empty();
		private Mono<OrderResponseDto> orderResponse = Mono.empty();

		@Override
		public Mono<OrderResponseDto> buy(org.springframework.web.server.WebSession session) {
			return buyResponse;
		}

		@Override
		public Flux<OrderResponseDto> getOrders() {
			return ordersFlux;
		}

		@Override
		public Mono<OrderResponseDto> getOrder(long id) {
			return orderResponse;
		}
	}
}
