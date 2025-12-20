package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import ru.yandex.practicum.mymarket.entity.OrderItemEntity;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItemEntity, Long> {

	Flux<OrderItemEntity> findByOrderId(Long orderId);
}
