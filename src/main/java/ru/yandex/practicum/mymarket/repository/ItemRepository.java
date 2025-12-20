package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;

import ru.yandex.practicum.mymarket.entity.ItemEntity;

public interface ItemRepository extends ReactiveCrudRepository<ItemEntity, Long>, ReactiveSortingRepository<ItemEntity, Long> {

	Flux<ItemEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
}
