package ru.yandex.practicum.mymarket.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemImageEntity;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

class ItemImageInitializerTest {

	@Test
	void fillsMissingImagesFromClasspath() {
		StubItemRepository itemRepo = new StubItemRepository();
		itemRepo.items.add(new ItemEntity(1L, "t", "d", 10L, "images/android_phone.png"));
		StubItemImageRepository imageRepo = new StubItemImageRepository();

		ItemImageInitializer initializer = new ItemImageInitializer(itemRepo, imageRepo);
		initializer.fillImagesIfMissing();

		assertTrue(imageRepo.storage.containsKey(1L));
		byte[] data = imageRepo.storage.get(1L).getData();
		assertTrue(data != null && data.length > 0);
	}

	private static class StubItemRepository implements ItemRepository {
		private final List<ItemEntity> items = new ArrayList<>();

		@Override
		public Flux<ItemEntity> findAll() {
			return Flux.fromIterable(items);
		}

		@Override
		public <S extends ItemEntity> Mono<S> save(S entity) {
			items.add(entity);
			return Mono.just(entity);
		}

		@Override
		public <S extends ItemEntity> Flux<S> saveAll(Iterable<S> entities) {
			entities.forEach(items::add);
			return Flux.fromIterable((Iterable<S>) items);
		}

		@Override
		public <S extends ItemEntity> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
			return Flux.from(entityStream).doOnNext(items::add);
		}

		@Override
		public Mono<ItemEntity> findById(Long aLong) {
			return Mono.empty();
		}

		@Override
		public Mono<ItemEntity> findById(org.reactivestreams.Publisher<Long> idPublisher) {
			return Mono.empty();
		}

		@Override
		public Flux<ItemEntity> findAllById(Iterable<Long> longs) {
			return Flux.empty();
		}

		@Override
		public Flux<ItemEntity> findAllById(org.reactivestreams.Publisher<Long> idPublisher) {
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
			return Mono.just((long) items.size());
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
			items.clear();
			return Mono.empty();
		}

		@Override
		public reactor.core.publisher.Flux<ItemEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description) {
			return Flux.empty();
		}

		@Override
		public reactor.core.publisher.Flux<ItemEntity> findAll(org.springframework.data.domain.Sort sort) {
			return Flux.fromIterable(items);
		}
	}

	private static class StubItemImageRepository implements ItemImageRepository {
		private final Map<Long, ItemImageEntity> storage = new ConcurrentHashMap<>();

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
}
