package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.UserEntity;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, Long> {

	Mono<UserEntity> findByUsername(String username);

	@Modifying
	@Query("UPDATE users SET balance = balance - :amount WHERE id = :userId AND balance >= :amount")
	Mono<Integer> deductBalance(Long userId, Long amount);
}
