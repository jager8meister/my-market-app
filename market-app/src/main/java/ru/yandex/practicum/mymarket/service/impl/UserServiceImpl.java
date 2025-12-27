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
	@Transactional
	public Mono<UserEntity> getCurrentUser() {
		return ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication)
				.flatMap(auth -> {
					String username = extractUsername(auth);
					log.debug("Extracted username from authentication: {}", username);
					return userRepository.findByUsername(username)
							.switchIfEmpty(Mono.defer(() -> createUserIfNotExists(username)));
				})
				.switchIfEmpty(Mono.error(new UserNotFoundException("Current user not found")))
				.doOnSuccess(user -> log.debug("Current user: {}", user.getUsername()));
	}

	private String extractUsername(Authentication auth) {
		if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
			org.springframework.security.oauth2.core.user.OAuth2User oauth2User =
				(org.springframework.security.oauth2.core.user.OAuth2User) auth.getPrincipal();
			String username = oauth2User.getAttribute("preferred_username");
			if (username == null) {
				username = oauth2User.getAttribute("name");
			}
			if (username == null) {
				username = auth.getName();
			}
			return username;
		}
		return auth.getName();
	}

	@Transactional
	private Mono<UserEntity> createUserIfNotExists(String username) {
		log.info("User {} not found in local DB, creating...", username);
		UserEntity newUser = new UserEntity();
		newUser.setUsername(username);
		newUser.setPassword("");
		return userRepository.save(newUser)
				.doOnSuccess(user -> log.info("Created new user {} with ID {}", username, user.getId()));
	}

	@Override
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
