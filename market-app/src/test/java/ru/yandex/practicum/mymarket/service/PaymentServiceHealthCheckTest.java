package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.test.StepVerifier;

class PaymentServiceHealthCheckTest {

	private MockWebServer mockWebServer;
	private PaymentServiceHealthCheck healthCheck;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		String baseUrl = mockWebServer.url("/").toString();
		healthCheck = new PaymentServiceHealthCheck(WebClient.builder(), baseUrl);
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void shouldBeAvailableInitially() {
		assertTrue(healthCheck.isPaymentServiceAvailable());
	}

	@Test
	void shouldMarkAsAvailableWhenHealthCheckSucceeds() {
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));

		StepVerifier.create(healthCheck.checkHealth())
				.verifyComplete();

		assertTrue(healthCheck.isPaymentServiceAvailable());
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

	@Test
	void shouldRecoverWhenServiceBecomesAvailable() {
		mockWebServer.enqueue(new MockResponse().setResponseCode(500));
		StepVerifier.create(healthCheck.checkHealth())
				.verifyComplete();

		assertFalse(healthCheck.isPaymentServiceAvailable());

		mockWebServer.enqueue(new MockResponse().setResponseCode(200));
		StepVerifier.create(healthCheck.checkHealth())
				.verifyComplete();

		assertTrue(healthCheck.isPaymentServiceAvailable());
	}
}
