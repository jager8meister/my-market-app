package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.CartItemEntity;

public interface CartItemRepository extends ReactiveCrudRepository<CartItemEntity, Long> {

	Flux<CartItemEntity> findByUserId(Long userId);

	Mono<CartItemEntity> findByUserIdAndItemId(Long userId, Long itemId);

	Mono<Void> deleteByUserId(Long userId);

	Mono<Void> deleteByUserIdAndItemId(Long userId, Long itemId);
}
