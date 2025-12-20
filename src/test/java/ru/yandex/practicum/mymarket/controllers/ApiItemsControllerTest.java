package ru.yandex.practicum.mymarket.controllers;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

class ApiItemsControllerTest {

	private StubItemService itemService;

	private StubCartService cartService;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		itemService = new StubItemService();
		cartService = new StubCartService();
		ApiItemsController controller = new ApiItemsController(itemService, cartService);
		webTestClient = WebTestClient.bindToController(controller).build();
	}

	@Test
	void getItems_returnsItemsFromService() {
		ItemResponseDto dto = new ItemResponseDto(1L, "Title", "Desc", "img", 100L, 0);
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
		org.springframework.data.domain.Page<ItemResponseDto> page =
			new org.springframework.data.domain.PageImpl<>(java.util.List.of(dto), pageable, 1);
		itemService.itemsPage = Mono.just(page);

		webTestClient.get()
				.uri("/api/items?search=phone&pageNumber=1&pageSize=5")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content").isArray()
				.jsonPath("$.content.length()").isEqualTo(1);
	}

	@Test
	void postItems_updatesCart() {
		CartItemResponseDto item = new CartItemResponseDto(1L, "Title", "Desc", "img", 100L, 1);
		CartStateResponseDto cartState = new CartStateResponseDto(List.of(item), 100L);
		cartService.cartResponse = Mono.just(cartState);

		webTestClient.post()
				.uri("/api/items")
				.body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
				.exchange()
				.expectStatus().isOk()
				.expectBody(CartStateResponseDto.class)
				.isEqualTo(cartState);
	}

	@Test
	void getItem_returnsDetails() {
		ItemDetailsResponseDto details = new ItemDetailsResponseDto(2L, "Title", "Desc", "img", 200L, 0);
		itemService.itemDetails = Mono.just(details);

		webTestClient.get()
				.uri("/api/items/2")
				.exchange()
				.expectStatus().isOk()
				.expectBody(ItemDetailsResponseDto.class)
				.isEqualTo(details);
	}

	@Test
	void postItemId_updatesCart() {
		CartStateResponseDto state = new CartStateResponseDto(List.of(), 0);
		cartService.cartResponse = Mono.just(state);

		webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path("/api/items/3").queryParam("action", "DELETE").build())
				.exchange()
				.expectStatus().isOk()
				.expectBody(CartStateResponseDto.class)
				.isEqualTo(state);
	}

	@Test
	void getItemImage_returnsBinary() {
		byte[] data = new byte[] {1, 2, 3};
		itemService.imageResponse = Mono.just(ResponseEntity.ok(data));

		webTestClient.get()
				.uri("/api/items/4/image")
				.exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class)
				.isEqualTo(data);
	}

	private static class StubItemService implements ItemService {
		private Mono<org.springframework.data.domain.Page<ItemResponseDto>> itemsPage = Mono.empty();
		private Mono<ItemDetailsResponseDto> itemDetails = Mono.empty();
		private Mono<ResponseEntity<byte[]>> imageResponse = Mono.empty();

		@Override
		public Mono<org.springframework.data.domain.Page<ItemResponseDto>> getItems(ItemsFilterRequestDto filter, org.springframework.data.domain.Pageable pageable) {
			return itemsPage;
		}

		@Override
		public Mono<ItemDetailsResponseDto> getItem(Long id) {
			return itemDetails;
		}

		@Override
		public Mono<ResponseEntity<byte[]>> getItemImageResponse(Long id) {
			return imageResponse;
		}
	}

	private static class StubCartService implements CartService {
		private Mono<CartStateResponseDto> updateResponse = Mono.empty();
		private Mono<CartStateResponseDto> cartResponse = Mono.empty();

		@Override
		public Mono<Void> applyCartAction(CartAction action, Long itemId, org.springframework.web.server.WebSession session) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> clear(org.springframework.web.server.WebSession session) {
			return Mono.empty();
		}

		@Override
		public Flux<ru.yandex.practicum.mymarket.service.model.CartEntry> getItems(org.springframework.web.server.WebSession session) {
			return Flux.empty();
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
		public Mono<CartStateResponseDto> updateCart(ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto request, org.springframework.web.server.WebSession session) {
			return updateResponse;
		}
	}
}
