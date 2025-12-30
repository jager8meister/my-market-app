package ru.yandex.practicum.mymarket.service.balance;

import reactor.core.publisher.Mono;

public interface UserBalanceService {

	Mono<Long> getUserBalance(Long userId);

	Mono<Boolean> hasEnoughBalance(Long userId, Long amount);
}
