package ru.yandex.practicum.payment.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.BalancesApi;
import ru.yandex.practicum.payment.model.BalanceResponse;
import ru.yandex.practicum.payment.service.BalanceService;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class BalanceController implements BalancesApi {

	private final BalanceService balanceService;

	@Override
	public Mono<ResponseEntity<BalanceResponse>> getUserBalance(
			Long userId,
			ServerWebExchange exchange) {
		log.debug("getUserBalance called for userId: {}", userId);

		return balanceService.getUserBalance(userId)
				.map(balance -> {
					BalanceResponse response = new BalanceResponse();
					response.setUserId(userId);
					response.setBalance(balance);
					return ResponseEntity.ok(response);
				})
				.doOnSuccess(response -> log.debug("Returning balance {} for user {}",
						response.getBody().getBalance(), userId));
	}
}
