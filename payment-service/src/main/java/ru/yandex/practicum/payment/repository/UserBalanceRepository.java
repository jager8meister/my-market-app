package ru.yandex.practicum.payment.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.entity.UserBalanceEntity;

@Repository
public interface UserBalanceRepository extends ReactiveCrudRepository<UserBalanceEntity, Long> {

	@Modifying
	@Query("UPDATE user_balances SET balance = balance - :amount, updated_at = CURRENT_TIMESTAMP " +
			"WHERE user_id = :userId AND balance >= :amount")
	Mono<Integer> deductBalance(Long userId, Long amount);

	@Modifying
	@Query("INSERT INTO user_balances (user_id, balance, created_at, updated_at) " +
			"VALUES (:userId, :amount, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
			"ON CONFLICT (user_id) DO UPDATE SET " +
			"balance = user_balances.balance + :amount, " +
			"updated_at = CURRENT_TIMESTAMP")
	Mono<Integer> addBalance(Long userId, Long amount);
}
