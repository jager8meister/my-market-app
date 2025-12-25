package ru.yandex.practicum.mymarket.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ResilienceConfig {

	@Bean
	public CircuitBreakerRegistry circuitBreakerRegistry() {
		CircuitBreakerConfig config = CircuitBreakerConfig.custom()
				.failureRateThreshold(50)
				.slowCallRateThreshold(50)
				.slowCallDurationThreshold(Duration.ofSeconds(3))
				.slidingWindowSize(10)
				.minimumNumberOfCalls(5)
				.waitDurationInOpenState(Duration.ofSeconds(10))
				.automaticTransitionFromOpenToHalfOpenEnabled(true)
				.build();
		return CircuitBreakerRegistry.of(config);
	}

	@Bean
	public CircuitBreaker paymentServiceCircuitBreaker(CircuitBreakerRegistry registry) {
		CircuitBreaker circuitBreaker = registry.circuitBreaker("paymentService");

		circuitBreaker.getEventPublisher()
				.onStateTransition(event -> log.warn("Circuit Breaker: {} -> {}",
						event.getStateTransition().getFromState(),
						event.getStateTransition().getToState()))
				.onFailureRateExceeded(
						event -> log.error("Circuit Breaker failure rate exceeded: {}%", event.getFailureRate()));

		return circuitBreaker;
	}
}
