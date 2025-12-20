package ru.yandex.practicum.mymarket.controllers;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;

class ApiCartControllerTest {

	private StubCartService cartService;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		cartService = new StubCartService();
		ApiCartController controller = new ApiCartController(cartService);
		webTestClient = WebTestClient.bindToController(controller).build();
	}

	@Test
	void getCart_returnsState() {
		CartStateResponseDto state = new CartStateResponseDto(List.of(), 0);
		cartService.cartResponse = Mono.just(state);

		webTestClient.get()
				.uri("/api/cart/items")
				.exchange()
				.expectStatus().isOk()
				.expectBody(CartStateResponseDto.class)
				.isEqualTo(state);
	}

	@Test
	void postCart_updatesCart() {
		CartItemResponseDto item = new CartItemResponseDto(5L, "Title", "Desc", "img", 150L, 2);
		CartStateResponseDto state = new CartStateResponseDto(List.of(item), 300L);
		cartService.updateResponse = Mono.just(state);

		webTestClient.post()
				.uri("/api/cart/items")
				.body(BodyInserters.fromFormData("id", "5").with("action", CartAction.PLUS.name()))
				.exchange()
				.expectStatus().isOk()
				.expectBody(CartStateResponseDto.class)
				.isEqualTo(state);
	}

	private static class StubCartService implements CartService {
		private Mono<CartStateResponseDto> cartResponse = Mono.empty();
		private Mono<CartStateResponseDto> updateResponse = Mono.empty();

		@Override
		public Mono<Void> applyCartAction(CartAction action, Long itemId, org.springframework.web.server.WebSession session) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> clear(org.springframework.web.server.WebSession session) {
			return Mono.empty();
		}

		@Override
		public reactor.core.publisher.Flux<ru.yandex.practicum.mymarket.service.model.CartEntry> getItems(org.springframework.web.server.WebSession session) {
			return reactor.core.publisher.Flux.empty();
		}

		@Override
		public Mono<Long> getTotalPrice(org.springframework.web.server.WebSession session) {
			return Mono.empty();
		}

		@Override
		public Mono<CartStateResponseDto> getCart(org.springframework.web.server.WebSession session) {
			return cartResponse;
		}

		@Override
		public Mono<CartStateResponseDto> updateCart(CartUpdateRequestDto request, org.springframework.web.server.WebSession session) {
			return updateResponse;
		}

		@Override
		public Mono<Integer> getItemCountInCart(Long itemId, org.springframework.web.server.WebSession session) {
			return Mono.just(0);
		}
	}
}
