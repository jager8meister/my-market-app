package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.transaction.reactive.TransactionCallback;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.WebSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.mapper.OrderMapperImpl;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.service.impl.OrderServiceImpl;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

class OrderServiceImplTest {

	private StubOrderRepository orderRepository;
	private StubOrderItemRepository orderItemRepository;
	private StubCartService cartService;

	private OrderService orderService;

	private WebSession session;

	@BeforeEach
	void setUp() {
		orderRepository = new StubOrderRepository();
		orderItemRepository = new StubOrderItemRepository();
		cartService = new StubCartService();

		org.springframework.transaction.reactive.TransactionalOperator transactionalOperator = new StubTransactionalOperator();

		orderService = new OrderServiceImpl(orderRepository, orderItemRepository, cartService, new OrderMapperImpl(), transactionalOperator);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
		session = exchange.getSession().block();
	}

	@Test
	void buy_createsOrderAndClearsCart() {
		ItemEntity item = new ItemEntity(1L, "Item", "Desc", 150L, "img");
		cartService.items = List.of(new CartEntry(item, 2));

		StepVerifier.create(orderService.buy(session))
				.assertNext(resp -> {
					assertEquals(300L, resp.totalSum());
					assertNotNull(resp.createdAt());
					assertEquals(1, resp.items().size());
				})
				.verifyComplete();

		assertEquals(1, cartService.clearCalls);
	}

	@Test
	void getOrders_returnsAll() {
		OrderEntity first = orderRepository.saveSync(new OrderEntity(null, 1000L, LocalDateTime.now().minusDays(1)));
		OrderEntity second = orderRepository.saveSync(new OrderEntity(null, 500L, LocalDateTime.now()));
		orderItemRepository.saveSync(new OrderItemEntity(null, first.getId(), "A", 200L, 5));
		orderItemRepository.saveSync(new OrderItemEntity(null, second.getId(), "B", 100L, 5));

		StepVerifier.create(orderService.getOrders().collectList())
				.assertNext(list -> assertEquals(2, list.size()))
				.verifyComplete();
	}

	@Test
	void getOrder_returnsSingle() {
		OrderEntity order = orderRepository.saveSync(new OrderEntity(null, 200L, LocalDateTime.now()));
		orderItemRepository.saveSync(new OrderItemEntity(null, order.getId(), "Name", 100L, 2));

		StepVerifier.create(orderService.getOrder(order.getId()))
				.expectNextMatches(resp -> resp.id() == order.getId() && resp.items().size() == 1)
				.verifyComplete();
	}

	@Test
	void getOrder_notFoundThrows() {
		StepVerifier.create(orderService.getOrder(404L))
				.expectError(OrderNotFoundException.class)
				.verify();
	}

	@Test
	void buy_emptyCartThrows() {
		cartService.items = List.of();
		StepVerifier.create(orderService.buy(session))
				.expectError()
				.verify();
	}

	private static class StubOrderRepository implements OrderRepository {
		private final Map<Long, OrderEntity> storage = new ConcurrentHashMap<>();
		private long seq = 1;

		OrderEntity saveSync(OrderEntity entity) {
			if (entity.getId() == null) {
				entity.setId(seq++);
			}
			if (entity.getCreatedAt() == null) {
				entity.setCreatedAt(LocalDateTime.now());
			}
			storage.put(entity.getId(), entity);
			return entity;
		}

		@Override
		public Mono<OrderEntity> findById(Long aLong) {
			return Mono.justOrEmpty(storage.get(aLong));
		}

		@Override
		public Mono<OrderEntity> findById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.from(idPublisher).flatMap(this::findById);
		}

		@Override
		public Flux<OrderEntity> findAll() {
			return Flux.fromIterable(storage.values());
		}

		@Override
		public Flux<OrderEntity> findAllById(Iterable<Long> longs) {
			List<OrderEntity> list = new ArrayList<>();
			for (Long id : longs) {
				if (storage.containsKey(id)) {
					list.add(storage.get(id));
				}
			}
			return Flux.fromIterable(list);
		}

		@Override
		public Flux<OrderEntity> findAllById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Flux.from(idPublisher).flatMap(this::findById);
		}

		@Override
		public Mono<Long> count() {
			return Mono.just((long) storage.size());
		}

		@Override
		public Mono<Void> deleteById(Long aLong) {
			storage.remove(aLong);
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.from(idPublisher).doOnNext(storage::remove).then();
		}

		@Override
		public Mono<Void> delete(OrderEntity entity) {
			storage.remove(entity.getId());
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAllById(Iterable<? extends Long> longs) {
			longs.forEach(storage::remove);
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(Iterable<? extends OrderEntity> entities) {
			entities.forEach(e -> storage.remove(e.getId()));
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends OrderEntity> entityStream) {
			return Mono.from(entityStream).doOnNext(e -> storage.remove(e.getId())).then();
		}

		@Override
		public Mono<Void> deleteAll() {
			storage.clear();
			return Mono.empty();
		}

		@Override
		public <S extends OrderEntity> Mono<S> save(S entity) {
			saveSync(entity);
			return Mono.just(entity);
		}

		@Override
		public <S extends OrderEntity> Flux<S> saveAll(Iterable<S> entities) {
			entities.forEach(this::saveSync);
			return Flux.fromIterable(entities);
		}

		@Override
		public <S extends OrderEntity> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
			return Flux.from(entityStream).doOnNext(this::saveSync);
		}

		@Override
		public Mono<Boolean> existsById(Long aLong) {
			return Mono.just(storage.containsKey(aLong));
		}

		@Override
		public Mono<Boolean> existsById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.from(idPublisher).map(storage::containsKey);
		}

		@Override
		public Flux<OrderEntity> findAllByOrderByCreatedAtDesc() {
			return Flux.fromStream(storage.values().stream()
					.sorted(Comparator.comparing(OrderEntity::getCreatedAt).reversed()));
		}
	}

	private static class StubOrderItemRepository implements OrderItemRepository {
		private final List<OrderItemEntity> storage = new ArrayList<>();
		private long seq = 1;

		OrderItemEntity saveSync(OrderItemEntity entity) {
			if (entity.getId() == null) {
				entity.setId(seq++);
			}
			storage.add(entity);
			return entity;
		}

		@Override
		public Flux<OrderItemEntity> findByOrderId(Long orderId) {
			return Flux.fromStream(storage.stream().filter(e -> e.getOrderId().equals(orderId)));
		}

		@Override
		public <S extends OrderItemEntity> Mono<S> save(S entity) {
			saveSync(entity);
			return Mono.just(entity);
		}

		@Override
		public <S extends OrderItemEntity> Flux<S> saveAll(Iterable<S> entities) {
			entities.forEach(this::saveSync);
			return Flux.fromIterable(entities);
		}

		@Override
		public <S extends OrderItemEntity> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
			return Flux.from(entityStream).doOnNext(this::saveSync);
		}

		@Override
		public Mono<OrderItemEntity> findById(Long aLong) {
			return Mono.empty();
		}

		@Override
		public Mono<OrderItemEntity> findById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.empty();
		}

		@Override
		public Flux<OrderItemEntity> findAll() {
			return Flux.fromIterable(storage);
		}

		@Override
		public Flux<OrderItemEntity> findAllById(Iterable<Long> longs) {
			return Flux.empty();
		}

		@Override
		public Flux<OrderItemEntity> findAllById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Flux.empty();
		}

		@Override
		public Mono<Boolean> existsById(Long aLong) {
			return Mono.just(false);
		}

		@Override
		public Mono<Boolean> existsById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.just(false);
		}

		@Override
		public Mono<Long> count() {
			return Mono.just((long) storage.size());
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
		public Mono<Void> delete(OrderItemEntity entity) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAllById(Iterable<? extends Long> longs) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(Iterable<? extends OrderItemEntity> entities) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends OrderItemEntity> entityStream) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll() {
			storage.clear();
			return Mono.empty();
		}
	}

	private static class StubCartService implements CartService {
		private List<CartEntry> items = List.of();
		private int clearCalls = 0;

		@Override
		public Mono<Void> applyCartAction(ru.yandex.practicum.mymarket.enums.CartAction action, Long itemId, WebSession session) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> clear(WebSession session) {
			clearCalls++;
			return Mono.empty();
		}

		@Override
		public Flux<CartEntry> getItems(WebSession session) {
			return Flux.fromIterable(items);
		}

		@Override
		public Mono<Long> getTotalPrice(WebSession session) {
			return Mono.just(items.stream().mapToLong(entry -> entry.getItem().getPrice() * entry.getCount()).sum());
		}

		@Override
		public Mono<ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto> getCart(WebSession session) {
			return Mono.empty();
		}

		@Override
		public Mono<ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto> updateCart(ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto request, WebSession session) {
			return Mono.empty();
		}

		@Override
		public Mono<Integer> getItemCountInCart(Long itemId, WebSession session) {
			return Mono.just(0);
		}
	}

	private static class StubTransactionalOperator implements TransactionalOperator {
		@Override
		public <T> Flux<T> execute(TransactionCallback<T> action) {
			return Flux.from(action.doInTransaction(null));
		}
	}
}
