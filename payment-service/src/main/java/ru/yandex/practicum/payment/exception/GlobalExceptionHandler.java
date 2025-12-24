package ru.yandex.practicum.payment.exception;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.model.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(PaymentNotFoundException.class)
	public Mono<ResponseEntity<ErrorResponse>> handlePaymentNotFound(
			PaymentNotFoundException ex,
			ServerWebExchange exchange) {
		log.warn("Payment not found: {}", ex.getMessage());

		ErrorResponse error = new ErrorResponse();
		error.setError("Not Found");
		error.setMessage(ex.getMessage());
		error.setTimestamp(OffsetDateTime.now());
		error.setPath(exchange.getRequest().getPath().value());

		return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
	}

	@ExceptionHandler(PaymentOperationException.class)
	public Mono<ResponseEntity<ErrorResponse>> handlePaymentOperation(
			PaymentOperationException ex,
			ServerWebExchange exchange) {
		log.warn("Payment operation error: {}", ex.getMessage());

		ErrorResponse error = new ErrorResponse();
		error.setError("Bad Request");
		error.setMessage(ex.getMessage());
		error.setTimestamp(OffsetDateTime.now());
		error.setPath(exchange.getRequest().getPath().value());

		return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleUserNotFound(
			UserNotFoundException ex,
			ServerWebExchange exchange) {
		log.warn("User not found: {}", ex.getMessage());

		ErrorResponse error = new ErrorResponse();
		error.setError("Not Found");
		error.setMessage(ex.getMessage());
		error.setTimestamp(OffsetDateTime.now());
		error.setPath(exchange.getRequest().getPath().value());

		return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleInsufficientBalance(
			InsufficientBalanceException ex,
			ServerWebExchange exchange) {
		log.warn("Insufficient balance: {}", ex.getMessage());

		ErrorResponse error = new ErrorResponse();
		error.setError("Payment Required");
		error.setMessage(ex.getMessage());
		error.setTimestamp(OffsetDateTime.now());
		error.setPath(exchange.getRequest().getPath().value());

		return Mono.just(ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error));
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleValidation(
			WebExchangeBindException ex,
			ServerWebExchange exchange) {
		log.warn("Validation error: {}", ex.getMessage());

		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");

		ErrorResponse error = new ErrorResponse();
		error.setError("Bad Request");
		error.setMessage(message);
		error.setTimestamp(OffsetDateTime.now());
		error.setPath(exchange.getRequest().getPath().value());

		return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolation(
			ConstraintViolationException ex,
			ServerWebExchange exchange) {
		log.warn("Constraint violation: {}", ex.getMessage());

		String message = ex.getConstraintViolations().stream()
				.findFirst()
				.map(violation -> violation.getMessage())
				.orElse("Constraint violation");

		ErrorResponse error = new ErrorResponse();
		error.setError("Bad Request");
		error.setMessage(message);
		error.setTimestamp(OffsetDateTime.now());
		error.setPath(exchange.getRequest().getPath().value());

		return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
	}

	@ExceptionHandler(Exception.class)
	public Mono<ResponseEntity<ErrorResponse>> handleGeneral(
			Exception ex,
			ServerWebExchange exchange) {
		log.error("Unexpected error occurred", ex);

		ErrorResponse error = new ErrorResponse();
		error.setError("Internal Server Error");
		error.setMessage("An unexpected error occurred");
		error.setTimestamp(OffsetDateTime.now());
		error.setPath(exchange.getRequest().getPath().value());

		return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
	}
}
