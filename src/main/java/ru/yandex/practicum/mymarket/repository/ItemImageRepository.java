package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import ru.yandex.practicum.mymarket.entity.ItemImageEntity;

public interface ItemImageRepository extends ReactiveCrudRepository<ItemImageEntity, Long> {

	Mono<ItemImageEntity> findByItemId(Long itemId);
}
