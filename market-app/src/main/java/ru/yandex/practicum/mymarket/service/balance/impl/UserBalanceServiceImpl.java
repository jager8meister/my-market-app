package ru.yandex.practicum.mymarket.service.balance.impl;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.client.PaymentClient;
import ru.yandex.practicum.mymarket.service.balance.UserBalanceService;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserBalanceServiceImpl implements UserBalanceService {

	private final PaymentClient paymentClient;

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
