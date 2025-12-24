package ru.yandex.practicum.mymarket.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.client.PaymentClient;
import ru.yandex.practicum.mymarket.entity.UserEntity;
import ru.yandex.practicum.mymarket.exception.UserNotFoundException;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PaymentClient paymentClient;

	@Override
	@Transactional(readOnly = true)
	public Mono<UserEntity> getCurrentUser() {
		return ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication)
				.map(Authentication::getName)
				.flatMap(username -> userRepository.findByUsername(username))
				.switchIfEmpty(Mono.error(new UserNotFoundException("Current user not found")))
				.doOnSuccess(user -> log.debug("Current user: {}", user.getUsername()));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<Long> getCurrentUserId() {
		return getCurrentUser().map(UserEntity::getId);
	}

	@Override
	public Mono<Long> getUserBalance(Long userId) {
		log.debug("Getting balance for user {} from payment-service", userId);
		return paymentClient.getUserBalance(userId)
				.doOnSuccess(balance -> log.debug("Retrieved balance {} from payment-service for user {}", balance, userId))
				.onErrorResume(error -> {
					log.warn("Unable to fetch balance for user {}: {}", userId, error.getMessage());
					return Mono.just(-1L);
				});
	}

	@Override
	public Mono<Boolean> hasEnoughBalance(Long userId, Long amount) {
		return getUserBalance(userId)
				.map(balance -> {
					if (balance == -1L) {
						log.warn("Payment service unavailable, cannot check balance for user {}", userId);
						return false;
					}
					return balance >= amount;
				})
				.doOnNext(hasEnough -> log.debug("User {} has enough balance for {}: {}", userId, amount, hasEnough));
	}
}
