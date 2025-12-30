package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.entity.CartItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.mapper.CartMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.impl.CartServiceImpl;

class CartServiceImplTest {

	private CartItemRepository cartItemRepository;
	private ItemRepository itemRepository;
	private CartMapper cartMapper;
	private ItemService itemService;
	private UserService userService;
	private CartServiceImpl cartService;

	private WebSession session;
	private Map<String, CartItemEntity> cartItemStorage;

	@BeforeEach
	void setUp() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
		session = exchange.getSession().block();

		cartItemStorage = new ConcurrentHashMap<>();

		ItemEntity item = new ItemEntity(1L, "Test", "Desc", 100L, "img");

		itemRepository = new StubItemRepository(item);

		cartItemRepository = new CartItemRepository() {
			@Override
			public <S extends CartItemEntity> Mono<S> save(S entity) {
				if (entity.getId() == null) {
					entity.setId((long) (cartItemStorage.size() + 1));
				}
				String key = entity.getUserId() + "-" + entity.getItemId();
				cartItemStorage.put(key, entity);
				return Mono.just(entity);
			}

			@Override
			public <S extends CartItemEntity> Flux<S> saveAll(Iterable<S> entities) {
				return Flux.fromIterable(entities).flatMap(this::save);
			}

			@Override
			public <S extends CartItemEntity> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
				return Flux.from(entityStream).flatMap(this::save);
			}

			@Override
			public Mono<CartItemEntity> findById(Long id) {
				return Flux.fromIterable(cartItemStorage.values())
						.filter(e -> e.getId().equals(id))
						.next();
			}

			@Override
			public Mono<CartItemEntity> findById(org.reactivestreams.Publisher<Long> id) {
				return Mono.from(id).flatMap(this::findById);
			}

			@Override
			public Mono<Boolean> existsById(Long id) {
				return findById(id).hasElement();
			}

			@Override
			public Mono<Boolean> existsById(org.reactivestreams.Publisher<Long> id) {
				return Mono.from(id).flatMap(this::existsById);
			}

			@Override
			public Flux<CartItemEntity> findAll() {
				return Flux.fromIterable(cartItemStorage.values());
			}

			@Override
			public Flux<CartItemEntity> findAllById(Iterable<Long> ids) {
				return Flux.fromIterable(ids).flatMap(this::findById);
			}

			@Override
			public Flux<CartItemEntity> findAllById(org.reactivestreams.Publisher<Long> idStream) {
				return Flux.from(idStream).flatMap(this::findById);
			}

			@Override
			public Mono<Long> count() {
				return Mono.just((long) cartItemStorage.size());
			}

			@Override
			public Mono<Void> deleteById(Long id) {
				return findById(id).flatMap(entity -> {
					String key = entity.getUserId() + "-" + entity.getItemId();
					cartItemStorage.remove(key);
					return Mono.empty();
				});
			}

			@Override
			public Mono<Void> deleteById(org.reactivestreams.Publisher<Long> id) {
				return Mono.from(id).flatMap(this::deleteById);
			}

			@Override
			public Mono<Void> delete(CartItemEntity entity) {
				String key = entity.getUserId() + "-" + entity.getItemId();
				cartItemStorage.remove(key);
				return Mono.empty();
			}

			@Override
			public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
				return Flux.fromIterable(ids).flatMap(this::deleteById).then();
			}

			@Override
			public Mono<Void> deleteAll(Iterable<? extends CartItemEntity> entities) {
				return Flux.fromIterable(entities).flatMap(this::delete).then();
			}

			@Override
			public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends CartItemEntity> entityStream) {
				return Flux.from(entityStream).flatMap(this::delete).then();
			}

			@Override
			public Mono<Void> deleteAll() {
				cartItemStorage.clear();
				return Mono.empty();
			}

			@Override
			public Flux<CartItemEntity> findByUserId(Long userId) {
				return Flux.fromIterable(cartItemStorage.values())
						.filter(e -> e.getUserId().equals(userId));
			}

			@Override
			public Mono<CartItemEntity> findByUserIdAndItemId(Long userId, Long itemId) {
				String key = userId + "-" + itemId;
				return Mono.justOrEmpty(cartItemStorage.get(key));
			}

			@Override
			public Mono<Void> deleteByUserId(Long userId) {
				return findByUserId(userId)
						.flatMap(this::delete)
						.then();
			}

			@Override
			public Mono<Void> deleteByUserIdAndItemId(Long userId, Long itemId) {
				String key = userId + "-" + itemId;
				cartItemStorage.remove(key);
				return Mono.empty();
			}
		};

		cartMapper = entry -> new CartItemResponseDto(
			entry.getItem().getId(),
			entry.getItem().getTitle(),
			entry.getItem().getDescription(),
			entry.getItem().getImgPath(),
			entry.getItem().getPrice(),
			entry.getCount()
		);

		itemService = new ItemService() {
			@Override
			public Mono<ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto> getItem(Long id) {
				if (id.equals(1L)) {
					return Mono.just(new ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto(
						item.getId(),
						item.getTitle(),
						item.getDescription(),
						item.getImgPath(),
						item.getPrice(),
						0
					));
				}
				return Mono.empty();
			}
			@Override
			public Mono<org.springframework.data.domain.Page<ru.yandex.practicum.mymarket.dto.response.ItemResponseDto>> getItems(
				ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto filter,
				org.springframework.data.domain.Pageable pageable) {
				return Mono.empty();
			}
			@Override
			public Mono<org.springframework.data.domain.Page<ru.yandex.practicum.mymarket.dto.response.ItemResponseDto>> getItemsWithCartCounts(
				ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto filter,
				org.springframework.data.domain.Pageable pageable,
				ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto cart) {
				return Mono.empty();
			}
			@Override
			public Mono<ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto> getItemWithCartCount(Long id, int count) {
				return Mono.empty();
			}
			@Override
			public Mono<org.springframework.http.ResponseEntity<byte[]>> getItemImageResponse(Long id) {
				return Mono.empty();
			}
		};

		userService = new UserService() {
			@Override
			public Mono<ru.yandex.practicum.mymarket.entity.UserEntity> getCurrentUser() {
				return Mono.just(new ru.yandex.practicum.mymarket.entity.UserEntity(
						1L, "testuser", "password", 1000L, java.time.LocalDateTime.now()));
			}

			@Override
			public Mono<Long> getCurrentUserId() {
				return Mono.just(1L);
			}

			@Override
			public Mono<Boolean> hasEnoughBalance(Long userId, Long amount) {
				return Mono.just(true);
			}

			@Override
			public Mono<Long> getUserBalance(Long userId) {
				return Mono.just(1000L);
			}
		};

		cartService = new CartServiceImpl(cartItemRepository, itemRepository, cartMapper, itemService, userService);
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
		
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertEquals(3, state.items().get(0).count());

		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.MINUS), session).block();

		state = cartService.getCart(session).block();
		assertEquals(2, state.items().get(0).count());
		assertEquals(200L, state.total());
	}

	@Test
	void removeOneDeletesItemWhenCountIsOne() {
		
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertEquals(1, state.items().get(0).count());

		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.MINUS), session).block();

		state = cartService.getCart(session).block();
		assertTrue(state.items().isEmpty());
		assertEquals(0L, state.total());
	}

	@Test
	void removeOneFromEmptyCartDoesNothing() {
		
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.MINUS), session).block();

		CartStateResponseDto state = cartService.getCart(session).block();
		assertTrue(state.items().isEmpty());
	}

	@Test
	void removeOneNonExistentItemDoesNothing() {
		
		cartService.updateCart(new CartUpdateRequestDto(1L, CartAction.PLUS), session).block();

		cartService.updateCart(new CartUpdateRequestDto(999L, CartAction.MINUS), session).block();

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
