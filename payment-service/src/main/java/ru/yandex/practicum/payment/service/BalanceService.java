package ru.yandex.practicum.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.exception.InsufficientBalanceException;
import ru.yandex.practicum.payment.exception.UserNotFoundException;
import ru.yandex.practicum.payment.repository.UserBalanceRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BalanceService {

	private final UserBalanceRepository userBalanceRepository;

	@Transactional(readOnly = true)
	public Mono<Long> getUserBalance(Long userId) {
		log.debug("Getting balance for user {}", userId);
		return userBalanceRepository.findById(userId)
				.map(userBalance -> userBalance.getBalance())
				.switchIfEmpty(Mono.error(new UserNotFoundException("User balance not found for user: " + userId)))
				.doOnSuccess(balance -> log.debug("User {} balance: {}", userId, balance));
	}

	public Mono<Void> deductBalance(Long userId, Long amount) {
		log.debug("Deducting {} from user {} balance", amount, userId);
		return userBalanceRepository.deductBalance(userId, amount)
				.flatMap(rowsUpdated -> {
					if (rowsUpdated == 0) {
						log.warn("Failed to deduct balance for user {}: insufficient funds or user not found", userId);
						return Mono.error(new InsufficientBalanceException("Insufficient balance"));
					}
					log.debug("Successfully deducted {} from user {} balance", amount, userId);
					return Mono.empty();
				});
	}
}
