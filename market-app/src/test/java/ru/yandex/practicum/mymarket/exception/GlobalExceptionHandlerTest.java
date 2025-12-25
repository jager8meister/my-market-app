package ru.yandex.practicum.mymarket.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.practicum.mymarket.dto.response.ApiErrorResponse;

class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
	}

	@Test
	void handlesNotFound() {
		ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new ItemNotFoundException("not found"));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("not found", response.getBody().message());
	}

	@Test
	void handlesBadRequest() {
		ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(new EmptyCartException("empty"));
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("empty", response.getBody().message());
	}

	@Test
	void handlesImageInit() {
		ResponseEntity<ApiErrorResponse> response = handler.handleImageInitializationException(new ImageInitializationException("init failed"));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("init failed", response.getBody().message());
	}

	@Test
	void handlesGeneric() {
		ResponseEntity<ApiErrorResponse> response = handler.handleGenericException(new RuntimeException("boom"));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}
}
