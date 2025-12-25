package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.test.StepVerifier;

class PaymentServiceHealthCheckTest {

	private MockWebServer mockWebServer;
	private PaymentServiceHealthCheck healthCheck;
	private CircuitBreaker circuitBreaker;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		circuitBreaker = mock(CircuitBreaker.class, (Answer<?>) invocation -> CircuitBreaker.State.CLOSED);

		String baseUrl = mockWebServer.url("/").toString();
		healthCheck = new PaymentServiceHealthCheck(WebClient.builder(), baseUrl, circuitBreaker);
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void shouldBeUnavailableInitially() {
		assertFalse(healthCheck.isPaymentServiceAvailable());
	}

	@Test
	void shouldMarkAsUnavailableWhenHealthCheckFails() {
		mockWebServer.enqueue(new MockResponse().setResponseCode(500));

		StepVerifier.create(healthCheck.checkHealth())
				.verifyComplete();

		assertFalse(healthCheck.isPaymentServiceAvailable());
	}

	@Test
	void shouldMarkAsUnavailableOnTimeout() {
		mockWebServer.enqueue(new MockResponse()
				.setHeadersDelay(5, java.util.concurrent.TimeUnit.SECONDS));

		StepVerifier.create(healthCheck.checkHealth())
				.verifyComplete();

		assertFalse(healthCheck.isPaymentServiceAvailable());
	}
}
