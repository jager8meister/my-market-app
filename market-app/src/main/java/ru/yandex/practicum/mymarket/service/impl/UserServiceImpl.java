package ru.yandex.practicum.mymarket.service.impl;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.UserEntity;
import ru.yandex.practicum.mymarket.exception.UserNotFoundException;
import ru.yandex.practicum.mymarket.factory.UserFactory;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.security.OAuth2UserExtractor;
import ru.yandex.practicum.mymarket.service.UserService;
import ru.yandex.practicum.mymarket.service.balance.UserBalanceService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final OAuth2UserExtractor oauth2UserExtractor;
	private final UserFactory userFactory;
	private final UserBalanceService userBalanceService;

	@Override
	@Transactional
	public Mono<UserEntity> getCurrentUser() {
		return ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication)
				.flatMap(auth -> {
					String username = oauth2UserExtractor.extractUsername(auth);
					log.debug("Extracted username from authentication: {}", username);
					return userRepository.findByUsername(username)
							.switchIfEmpty(Mono.defer(() -> createUserIfNotExists(username)));
				})
				.switchIfEmpty(Mono.error(new UserNotFoundException("Current user not found")))
				.doOnSuccess(user -> log.debug("Current user: {}", user.getUsername()));
	}

	@Transactional
	private Mono<UserEntity> createUserIfNotExists(String username) {
		log.info("User {} not found in local DB, creating...", username);
		UserEntity newUser = userFactory.createNewOAuth2User(username);
		return userRepository.save(newUser)
				.doOnSuccess(user -> log.info("Created new user {} with ID {}", username, user.getId()));
	}

	@Override
	public Mono<Long> getCurrentUserId() {
		return getCurrentUser().map(UserEntity::getId);
	}

	@Override
	public Mono<Long> getUserBalance(Long userId) {
		return userBalanceService.getUserBalance(userId);
	}

	@Override
	public Mono<Boolean> hasEnoughBalance(Long userId, Long amount) {
		return userBalanceService.hasEnoughBalance(userId, amount);
	}
}
