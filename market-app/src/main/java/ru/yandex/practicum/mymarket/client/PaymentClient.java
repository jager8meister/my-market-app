package ru.yandex.practicum.mymarket.client;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.yandex.practicum.payment.client.api.BalancesApi;
import ru.yandex.practicum.payment.client.api.PaymentsApi;
import ru.yandex.practicum.payment.client.invoker.ApiClient;
import ru.yandex.practicum.payment.client.model.BalanceResponse;
import ru.yandex.practicum.payment.client.model.PaymentRequest;
import ru.yandex.practicum.payment.client.model.PaymentResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

	private final ApiClient apiClient;
	private final CircuitBreaker paymentServiceCircuitBreaker;

	public Mono<PaymentResponse> createPayment(Long orderId, Long userId, Long amount, String description) {
		log.info("Creating payment for order {}, user {}, amount: {}", orderId, userId, amount);

		PaymentRequest request = new PaymentRequest();
		request.setOrderId(orderId);
		request.setUserId(userId);
		request.setAmount(amount);
		request.setDescription(description);

		PaymentsApi paymentsApi = new PaymentsApi(apiClient);

		return paymentsApi.createPayment(request)
				.timeout(Duration.ofSeconds(10))
				.transformDeferred(CircuitBreakerOperator.of(paymentServiceCircuitBreaker))
				.retryWhen(Retry.backoff(3, Duration.ofMillis(500))
						.maxBackoff(Duration.ofSeconds(2))
						.doBeforeRetry(signal -> log.warn("Retrying payment creation for order {}, attempt {}",
								orderId, signal.totalRetries() + 1)))
				.doOnSuccess(response -> log.info("Payment created successfully: {}", response.getId()))
				.doOnError(error -> log.error("Failed to create payment for order {}: {}", orderId, error.getMessage()));
	}

	public Mono<PaymentResponse> getPayment(Long paymentId) {
		log.debug("Getting payment {}", paymentId);

		PaymentsApi paymentsApi = new PaymentsApi(apiClient);

		return paymentsApi.getPayment(paymentId)
				.doOnSuccess(response -> log.debug("Payment {} retrieved: status={}", paymentId, response.getStatus()))
				.doOnError(error -> log.error("Failed to get payment {}: {}", paymentId, error.getMessage()));
	}

	public Mono<PaymentResponse> cancelPayment(Long paymentId) {
		log.info("Cancelling payment {}", paymentId);

		PaymentsApi paymentsApi = new PaymentsApi(apiClient);

		return paymentsApi.cancelPayment(paymentId)
				.doOnSuccess(response -> log.info("Payment {} cancelled successfully", paymentId))
				.doOnError(error -> log.error("Failed to cancel payment {}: {}", paymentId, error.getMessage()));
	}

	public Mono<Long> getUserBalance(Long userId) {
		log.debug("Getting balance for user {}", userId);

		BalancesApi balancesApi = new BalancesApi(apiClient);

		return balancesApi.getUserBalance(userId)
				.map(BalanceResponse::getBalance)
				.timeout(Duration.ofSeconds(5))
				.transformDeferred(CircuitBreakerOperator.of(paymentServiceCircuitBreaker))
				.doOnSuccess(balance -> log.debug("User {} balance: {}", userId, balance))
				.doOnError(error -> log.error("Failed to get balance for user {}: {}", userId, error.getMessage()));
	}
}
