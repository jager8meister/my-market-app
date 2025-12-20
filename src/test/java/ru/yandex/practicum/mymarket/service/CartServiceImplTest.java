package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebSession;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.mapper.CartMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.impl.CartServiceImpl;

class CartServiceImplTest {

	private ItemRepository itemRepository;
	private CartMapper cartMapper;
	private CartServiceImpl cartService;

	private WebSession session;

	@BeforeEach
	void setUp() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
		session = exchange.getSession().block();

		ItemEntity item = new ItemEntity(1L, "Test", "Desc", 100L, "img");

		itemRepository = new StubItemRepository(item);
		cartMapper = entry -> new CartItemResponseDto(
			entry.getItem().getId(),
			entry.getItem().getTitle(),
			entry.getItem().getDescription(),
			entry.getItem().getImgPath(),
			entry.getItem().getPrice(),
			entry.getCount()
		);

		cartService = new CartServiceImpl(itemRepository, cartMapper);
	}

	@Test
	void shouldAddAndRemoveItemsInSessionCart() {
		CartStateResponseDto added = cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		assertEquals(1, added.items().size());
		assertEquals(100L, added.total());

		CartStateResponseDto removed = cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.MINUS), session).block();
		assertTrue(removed.items().isEmpty());
		assertEquals(0L, removed.total());
	}

	@Test
	void shouldCalculateTotalPrice() {
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();

		long total = cartService.getTotalPrice(session).block();
		assertEquals(200L, total);
	}

	@Test
	void shouldClearCart() {
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		cartService.clear(session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertTrue(state.items().isEmpty());
		assertEquals(0L, state.total());
	}

	@Test
	void shouldDeleteItem() {
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.DELETE), session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertTrue(state.items().isEmpty());
	}

	@Test
	void updateCartWithNullActionKeepsCartEmpty() {
		cartService.updateCart(new CartUpdateRequestDto(1L, null), session).block();
		assertTrue(cartService.getItems(session).collectList().block().isEmpty());
	}

	@Test
	void getItemsReturnsEmptyWhenCartMissing() {
		assertTrue(cartService.getItems(session).collectList().block().isEmpty());
	}

	@Test
	void removeOneDecreasesCountWhenMoreThanOne() {
		// Add item 3 times
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertEquals(3, state.items().get(0).count());

		// Remove one
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.MINUS), session).block();

		state = cartService.getCart(session).block();
		assertEquals(2, state.items().get(0).count());
		assertEquals(200L, state.total());
	}

	@Test
	void removeOneDeletesItemWhenCountIsOne() {
		// Add item once
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertEquals(1, state.items().get(0).count());

		// Remove one - should delete the item completely
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.MINUS), session).block();

		state = cartService.getCart(session).block();
		assertTrue(state.items().isEmpty());
		assertEquals(0L, state.total());
	}

	@Test
	void removeOneFromEmptyCartDoesNothing() {
		// Try to remove from empty cart
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.MINUS), session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertTrue(state.items().isEmpty());
	}

	@Test
	void removeOneNonExistentItemDoesNothing() {
		// Add item 1
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();

		// Try to remove non-existent item 999
		cartService.updateCart(new CartUpdateRequestDto(999L, CartAction.MINUS), session).block();

		// Item 1 should still be there
		CartStateResponseDto state = cartService.getCart(session).block();
		assertEquals(1, state.items().size());
		assertEquals(1, state.items().get(0).count());
	}

	private static class StubItemRepository implements ItemRepository {
		private final ItemEntity item;

		private StubItemRepository(ItemEntity item) {
			this.item = item;
		}

		@Override
		public Mono<ItemEntity> findById(Long aLong) {
			return item.getId().equals(aLong) ? Mono.just(item) : Mono.empty();
		}

		@Override
		public Mono<ItemEntity> findById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.from(idPublisher).flatMap(this::findById);
		}

		@Override
		public reactor.core.publisher.Flux<ItemEntity> findAllById(Iterable<Long> longs) {
			List<ItemEntity> list = new ArrayList<>();
			for (Long id : longs) {
				if (item.getId().equals(id)) {
					list.add(item);
				}
			}
			return reactor.core.publisher.Flux.fromIterable(list);
		}

		@Override
		public reactor.core.publisher.Flux<ItemEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description) {
			return reactor.core.publisher.Flux.just(item);
		}

		@Override
		public <S extends ItemEntity> Mono<S> save(S entity) {
			return Mono.just(entity);
		}

		@Override
		public <S extends ItemEntity> reactor.core.publisher.Flux<S> saveAll(Iterable<S> entities) {
			return reactor.core.publisher.Flux.fromIterable(entities);
		}

		@Override
		public <S extends ItemEntity> reactor.core.publisher.Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
			return reactor.core.publisher.Flux.from(entityStream);
		}

		@Override
		public Mono<Boolean> existsById(Long aLong) {
			return Mono.just(item.getId().equals(aLong));
		}

		@Override
		public Mono<Boolean> existsById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.from(idPublisher).map(item.getId()::equals);
		}

		@Override
		public reactor.core.publisher.Flux<ItemEntity> findAll() {
			return reactor.core.publisher.Flux.just(item);
		}

		@Override
		public reactor.core.publisher.Flux<ItemEntity> findAllById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.from(idPublisher)
					.flatMapMany(id -> item.getId().equals(id)
							? reactor.core.publisher.Flux.just(item)
							: reactor.core.publisher.Flux.empty());
		}

		@Override
		public Mono<Long> count() {
			return Mono.just(1L);
		}

		@Override
		public Mono<Void> deleteById(Long aLong) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> delete(ItemEntity entity) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAllById(Iterable<? extends Long> longs) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(Iterable<? extends ItemEntity> entities) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends ItemEntity> entityStream) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll() {
			return Mono.empty();
		}

		@Override
		public reactor.core.publisher.Flux<ItemEntity> findAll(org.springframework.data.domain.Sort sort) {
			return findAll();
		}
	}
}
