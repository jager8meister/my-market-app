package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.UserEntity;

public interface UserService {

	Mono<UserEntity> getCurrentUser();

	Mono<Long> getCurrentUserId();

	Mono<Long> getUserBalance(Long userId);

	Mono<Boolean> hasEnoughBalance(Long userId, Long amount);

	Mono<Void> deductBalance(Long userId, Long amount);
}
