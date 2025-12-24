package ru.yandex.practicum.payment.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class HomeController {

	@GetMapping("/")
	public Mono<Map<String, Object>> home() {
		return Mono.just(Map.of(
			"service", "Payment Service",
			"version", "1.0.0",
			"status", "running",
			"endpoints", Map.of(
				"balance", "/api/users/{userId}/balance",
				"payments", "/api/payments",
				"payment", "/api/payments/{id}"
			)
		));
	}
}
