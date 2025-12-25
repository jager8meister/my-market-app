package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import ru.yandex.practicum.mymarket.entity.OrderEntity;

public interface OrderRepository extends ReactiveCrudRepository<OrderEntity, Long> {

	Flux<OrderEntity> findAllByOrderByCreatedAtDesc();

	Flux<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
