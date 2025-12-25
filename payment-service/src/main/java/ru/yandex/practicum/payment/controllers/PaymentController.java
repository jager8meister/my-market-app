package ru.yandex.practicum.payment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.PaymentsApi;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;

@RestController
@RequiredArgsConstructor
@Validated
public class PaymentController implements PaymentsApi {

	private final PaymentService paymentService;

	@Override
	public Mono<ResponseEntity<PaymentResponse>> createPayment(
			Mono<PaymentRequest> paymentRequest,
			ServerWebExchange exchange) {
		return paymentRequest
				.flatMap(paymentService::createPayment)
				.map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
	}

	@Override
	public Mono<ResponseEntity<PaymentResponse>> getPayment(
			Long id,
			ServerWebExchange exchange) {
		return paymentService.getPayment(id)
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<PaymentResponse>> cancelPayment(
			Long id,
			ServerWebExchange exchange) {
		return paymentService.cancelPayment(id)
				.map(ResponseEntity::ok);
	}
}
