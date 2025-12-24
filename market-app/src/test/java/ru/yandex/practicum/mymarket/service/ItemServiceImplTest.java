package ru.yandex.practicum.mymarket.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemImageEntity;
import ru.yandex.practicum.mymarket.enums.SortType;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapperImpl;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.impl.ItemServiceImpl;

class ItemServiceImplTest {

	private StubItemRepository itemRepository;
	private StubItemImageRepository itemImageRepository;
	private StubReactiveCacheService cacheService;
	private ItemService itemService;

	@BeforeEach
	void setUp() {
		itemRepository = new StubItemRepository();
		itemImageRepository = new StubItemImageRepository();
		cacheService = new StubReactiveCacheService();
		itemService = new ItemServiceImpl(itemRepository, itemImageRepository, new ItemMapperImpl(), cacheService);
	}

	@Test
	void getItems_appliesSearchSortAndPaging() {
		itemRepository.saveSync(new ItemEntity(1L, "A Phone", "desc", 100L, "img1"));
		itemRepository.saveSync(new ItemEntity(2L, "C Phone", "desc", 300L, "img2"));
		itemRepository.saveSync(new ItemEntity(3L, "B Phone", "desc", 200L, "img3"));

		ItemsFilterRequestDto filter = new ItemsFilterRequestDto("phone", SortType.ALPHA);
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 2);

		StepVerifier.create(itemService.getItems(filter, pageable).map(Page::getContent))
				.assertNext(list -> {
					List<String> titles = list.stream().map(ItemResponseDto::title).toList();
					org.junit.jupiter.api.Assertions.assertEquals(List.of("A Phone", "B Phone"), titles);
				})
				.verifyComplete();
	}

	@Test
	void getItem_returnsDetails() {
		itemRepository.saveSync(new ItemEntity(4L, "Title", "Desc", 500L, "img"));

		StepVerifier.create(itemService.getItem(4L))
				.expectNextMatches(resp -> resp.id().equals(4L) && resp.title().equals("Title"))
				.verifyComplete();
	}

	@Test
	void getItem_notFoundThrows() {
		StepVerifier.create(itemService.getItem(404L))
				.expectError(ItemNotFoundException.class)
				.verify();
	}

	@Test
	void getItems_sortsByPrice() {
		itemRepository.saveSync(new ItemEntity(10L, "X", "desc", 300L, "img"));
		itemRepository.saveSync(new ItemEntity(11L, "Y", "desc", 100L, "img"));
		itemRepository.saveSync(new ItemEntity(12L, "Z", "desc", 200L, "img"));

		ItemsFilterRequestDto filter = new ItemsFilterRequestDto(null, SortType.PRICE);
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 3);

		StepVerifier.create(itemService.getItems(filter, pageable).map(Page::getContent))
				.assertNext(list -> {
					List<Long> prices = list.stream().map(ItemResponseDto::price).toList();
					org.junit.jupiter.api.Assertions.assertEquals(List.of(100L, 200L, 300L), prices);
				})
				.verifyComplete();
	}

	@Test
	void getItems_respectsPaginationOffset() {
		itemRepository.saveSync(new ItemEntity(20L, "A", "desc", 1L, "img"));
		itemRepository.saveSync(new ItemEntity(21L, "B", "desc", 2L, "img"));
		itemRepository.saveSync(new ItemEntity(22L, "C", "desc", 3L, "img"));

		ItemsFilterRequestDto filter = new ItemsFilterRequestDto(null, SortType.NO);
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(1, 2);

		StepVerifier.create(itemService.getItems(filter, pageable).map(Page::getContent))
				.assertNext(list -> org.junit.jupiter.api.Assertions.assertEquals(1, list.size()))
				.verifyComplete();
	}

	@Test
	void getItemImageResponse_returnsImage() {
		byte[] data = new byte[] {9, 8, 7};
		itemImageRepository.saveSync(new ItemImageEntity(1L, 5L, data, "image/png"));

		StepVerifier.create(itemService.getItemImageResponse(5L))
				.expectNextMatches(res -> {
					ResponseEntity<byte[]> response = res;
					return response.getStatusCode().is2xxSuccessful()
							&& response.getBody() != null
							&& response.getBody().length == 3;
				})
				.verifyComplete();
	}

	@Test
	void getItemImageResponse_notFoundThrows() {
		StepVerifier.create(itemService.getItemImageResponse(6L))
				.expectError(ItemNotFoundException.class)
				.verify();
	}

	private static class StubItemRepository implements ItemRepository {
		private final Map<Long, ItemEntity> storage = new ConcurrentHashMap<>();

		void saveSync(ItemEntity entity) {
			storage.put(entity.getId(), entity);
		}

		@Override
		public Flux<ItemEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description) {
			String search = title == null ? "" : title.toLowerCase(Locale.ROOT);
			List<ItemEntity> result = storage.values().stream()
					.filter(item -> item.getTitle().toLowerCase(Locale.ROOT).contains(search)
							|| item.getDescription().toLowerCase(Locale.ROOT).contains(search))
					.collect(Collectors.toList());
			return Flux.fromIterable(result);
		}

		@Override
		public <S extends ItemEntity> Mono<S> save(S entity) {
			saveSync(entity);
			return Mono.just(entity);
		}

		@Override
		public <S extends ItemEntity> Flux<S> saveAll(Iterable<S> entities) {
			List<S> saved = new ArrayList<>();
			for (S e : entities) {
				save(e).subscribe();
				saved.add(e);
			}
			return Flux.fromIterable(saved);
		}

		@Override
		public <S extends ItemEntity> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
			return Flux.from(entityStream).doOnNext(this::saveSync);
		}

		@Override
		public Mono<ItemEntity> findById(Long aLong) {
			return Mono.justOrEmpty(storage.get(aLong));
		}

		@Override
		public Mono<ItemEntity> findById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.from(idPublisher).flatMap(this::findById);
		}

		@Override
		public Flux<ItemEntity> findAll() {
			return Flux.fromIterable(storage.values());
		}

		@Override
		public Flux<ItemEntity> findAllById(Iterable<Long> longs) {
			List<ItemEntity> list = new ArrayList<>();
			for (Long id : longs) {
				if (storage.containsKey(id)) {
					list.add(storage.get(id));
				}
			}
			return Flux.fromIterable(list);
		}

		@Override
		public Flux<ItemEntity> findAllById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Flux.from(idPublisher).flatMap(this::findById);
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
		public Mono<Void> delete(ItemEntity entity) {
			storage.remove(entity.getId());
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAllById(Iterable<? extends Long> longs) {
			longs.forEach(storage::remove);
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(Iterable<? extends ItemEntity> entities) {
			entities.forEach(e -> storage.remove(e.getId()));
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends ItemEntity> entityStream) {
			return Mono.from(entityStream).doOnNext(e -> storage.remove(e.getId())).then();
		}

		@Override
		public Mono<Void> deleteAll() {
			storage.clear();
			return Mono.empty();
		}

		@Override
		public Flux<ItemEntity> findAll(Sort sort) {
			return findAll();
		}
	}

	private static class StubItemImageRepository implements ItemImageRepository {
		private final Map<Long, ItemImageEntity> storage = new ConcurrentHashMap<>();

		void saveSync(ItemImageEntity entity) {
			storage.put(entity.getItemId(), entity);
		}

		@Override
		public Mono<ItemImageEntity> findByItemId(Long itemId) {
			return Mono.justOrEmpty(storage.get(itemId));
		}

		@Override
		public <S extends ItemImageEntity> Mono<S> save(S entity) {
			storage.put(entity.getItemId(), entity);
			return Mono.just(entity);
		}

		@Override
		public <S extends ItemImageEntity> Flux<S> saveAll(Iterable<S> entities) {
			entities.forEach(e -> storage.put(e.getItemId(), e));
			return Flux.fromIterable(entities);
		}

		@Override
		public <S extends ItemImageEntity> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
			return Flux.from(entityStream).doOnNext(e -> storage.put(e.getItemId(), e));
		}

		@Override
		public Mono<ItemImageEntity> findById(Long aLong) {
			return Mono.empty();
		}

		@Override
		public Mono<ItemImageEntity> findById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.empty();
		}

		@Override
		public Flux<ItemImageEntity> findAll() {
			return Flux.fromIterable(storage.values());
		}

		@Override
		public Flux<ItemImageEntity> findAllById(Iterable<Long> longs) {
			return Flux.empty();
		}

		@Override
		public Flux<ItemImageEntity> findAllById(org.reactivestreams.Publisher<Long> idPublisher) {
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
		public Mono<Void> delete(ItemImageEntity entity) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAllById(Iterable<? extends Long> longs) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(Iterable<? extends ItemImageEntity> entities) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends ItemImageEntity> entityStream) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> deleteAll() {
			storage.clear();
			return Mono.empty();
		}
	}

	private static class StubReactiveCacheService extends ReactiveCacheService {
		public StubReactiveCacheService() {
			super(null);
		}

		@Override
		public <T> Mono<T> getOrPut(String key, Class<T> valueClass, Mono<T> dataSupplier, Duration ttl) {
			return dataSupplier;
		}

		@Override
		public Mono<Boolean> evict(String key) {
			return Mono.just(true);
		}

		@Override
		public Mono<Long> evictByPattern(String pattern) {
			return Mono.just(0L);
		}
	}
}
