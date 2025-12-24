package ru.yandex.practicum.mymarket.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.UserEntity;
import ru.yandex.practicum.mymarket.exception.InsufficientBalanceException;
import ru.yandex.practicum.mymarket.exception.UserNotFoundException;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;

	@Override
	public Mono<UserEntity> getCurrentUser() {
		return ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication)
				.map(Authentication::getName)
				.flatMap(username -> userRepository.findByUsername(username))
				.switchIfEmpty(Mono.error(new UserNotFoundException("Current user not found")))
				.doOnSuccess(user -> log.debug("Current user: {}", user.getUsername()));
	}

	@Override
	public Mono<Long> getCurrentUserId() {
		return getCurrentUser().map(UserEntity::getId);
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<Long> getUserBalance(Long userId) {
		log.debug("Getting balance for user {} from local DB", userId);
		return userRepository.findById(userId)
				.map(UserEntity::getBalance)
				.switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + userId)))
				.doOnSuccess(balance -> log.debug("Retrieved balance {} from local DB for user {}", balance, userId));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<Boolean> hasEnoughBalance(Long userId, Long amount) {
		return getUserBalance(userId)
				.map(balance -> balance >= amount)
				.doOnNext(hasEnough -> log.debug("User {} has enough balance for {}: {}", userId, amount, hasEnough));
	}

	@Override
	public Mono<Void> deductBalance(Long userId, Long amount) {
		log.debug("Deducting {} from user {} balance", amount, userId);
		return userRepository.deductBalance(userId, amount)
				.flatMap(rowsUpdated -> {
					if (rowsUpdated == 0) {
						log.warn("Failed to deduct balance for user {}: insufficient funds", userId);
						return Mono.error(new InsufficientBalanceException("Insufficient balance"));
					}
					log.debug("Successfully deducted {} from user {} balance", amount, userId);
					return Mono.empty();
				});
	}
}
