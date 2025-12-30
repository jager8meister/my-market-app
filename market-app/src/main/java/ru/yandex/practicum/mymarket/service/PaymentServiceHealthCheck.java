package ru.yandex.practicum.mymarket.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentServiceHealthCheck {

	private final WebClient webClient;
	private final String paymentServiceUrl;
	private final AtomicBoolean isAvailable = new AtomicBoolean(false);
	private final CircuitBreaker circuitBreaker;
	private Disposable healthCheckSubscription;

	public PaymentServiceHealthCheck(
			WebClient.Builder webClientBuilder,
			@Value("${payment.service.url}") String paymentServiceUrl,
			CircuitBreaker paymentServiceCircuitBreaker) {
		this.webClient = webClientBuilder.baseUrl(paymentServiceUrl).build();
		this.paymentServiceUrl = paymentServiceUrl;
		this.circuitBreaker = paymentServiceCircuitBreaker;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startHealthCheck() {
		log.info("Starting reactive health check for payment service");
		healthCheckSubscription = Flux.interval(Duration.ofSeconds(1), Duration.ofSeconds(10))
				.flatMap(tick -> checkHealth())
				.subscribe();
	}

	Mono<Void> checkHealth() {
		return webClient.get()
				.uri("/actuator/health")
				.retrieve()
				.toBodilessEntity()
				.timeout(Duration.ofSeconds(3))
				.doOnSuccess(response -> {
					boolean wasAvailable = isAvailable.getAndSet(true);
					if (!wasAvailable) {
						log.info("Payment service is now AVAILABLE");
					}
				})
				.doOnError(error -> {
					boolean wasAvailable = isAvailable.getAndSet(false);
					if (wasAvailable) {
						log.warn("Payment service is UNAVAILABLE: {}", error.getMessage());
					}
				})
				.then()
				.onErrorComplete();
	}

	public boolean isPaymentServiceAvailable() {
		boolean healthCheckStatus = isAvailable.get();
		CircuitBreaker.State state = circuitBreaker.getState();
		boolean circuitBreakerOk = state == CircuitBreaker.State.CLOSED
				|| state == CircuitBreaker.State.HALF_OPEN;

		boolean available = healthCheckStatus && circuitBreakerOk;

		if (!available) {
			log.debug("Payment service unavailable - Health: {}, CircuitBreaker: {}",
					healthCheckStatus, state);
		}

		return available;
	}

	@PreDestroy
	public void stopHealthCheck() {
		if (healthCheckSubscription != null && !healthCheckSubscription.isDisposed()) {
			healthCheckSubscription.dispose();
			log.info("Health check stopped");
		}
	}
}
