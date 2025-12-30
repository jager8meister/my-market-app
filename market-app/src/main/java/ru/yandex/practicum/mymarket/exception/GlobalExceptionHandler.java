package ru.yandex.practicum.mymarket.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.mymarket.dto.response.ApiErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({
			ItemNotFoundException.class,
			OrderNotFoundException.class,
			UserNotFoundException.class
	})
	public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
		log.warn("Not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
		log.warn("Insufficient balance: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
				.body(new ApiErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler({
			EmptyCartException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
		log.warn("Bad request: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(ImageInitializationException.class)
	public ResponseEntity<ApiErrorResponse> handleImageInitializationException(ImageInitializationException ex) {
		log.error("Image initialization error: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(PaymentException.class)
	public ResponseEntity<ApiErrorResponse> handlePaymentException(PaymentException ex) {
		log.error("Payment service error: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(new ApiErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
		log.warn("Service unavailable: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		log.warn("Access denied: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.findFirst()
				.map(DefaultMessageSourceResolvable::getDefaultMessage)
				.orElse("Validation failed");
		log.warn("Validation error: {}", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		String message = ex.getConstraintViolations()
				.stream()
				.findFirst()
				.map(violation -> violation.getMessage())
				.orElse("Constraint violation");
		log.warn("Constraint violation: {}", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
		log.error("Unexpected error occurred", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("An unexpected error occurred: " + ex.getMessage()));
	}
}
