package ru.yandex.practicum.mymarket.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import ru.yandex.practicum.mymarket.dto.response.ApiErrorResponse;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	@InjectMocks
	private GlobalExceptionHandler globalExceptionHandler;

	private static final String ITEM_NOT_FOUND_MESSAGE = "Item not found with id: 1";
	private static final String ORDER_NOT_FOUND_MESSAGE = "Order not found with id: 100";
	private static final String EMPTY_CART_MESSAGE = "Cannot checkout with empty cart";
	private static final String ILLEGAL_ARGUMENT_MESSAGE = "Invalid quantity: -5";
	private static final String IMAGE_INIT_MESSAGE = "Failed to initialize image storage";
	private static final String VALIDATION_ERROR_MESSAGE = "Name must not be blank";
	private static final String GENERIC_ERROR_MESSAGE = "Database connection failed";

	// ==================== handleNotFound() Tests ====================

	@Test
	void shouldReturn404WhenItemNotFoundExceptionIsThrown() {
		// Given
		ItemNotFoundException exception = new ItemNotFoundException(ITEM_NOT_FOUND_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(ITEM_NOT_FOUND_MESSAGE, response.getBody().getMessage());
	}

	@Test
	void shouldReturn404WhenOrderNotFoundExceptionIsThrown() {
		// Given
		OrderNotFoundException exception = new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(ORDER_NOT_FOUND_MESSAGE, response.getBody().getMessage());
	}


	@Test
	void shouldReturnCorrectMessageWhenNotFoundExceptionHasNullMessage() {
		// Given
		ItemNotFoundException exception = new ItemNotFoundException(null);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertNull(response.getBody().getMessage());
	}

	@Test
	void shouldReturnCorrectMessageWhenNotFoundExceptionHasEmptyMessage() {
		// Given
		String emptyMessage = "";
		ItemNotFoundException exception = new ItemNotFoundException(emptyMessage);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(emptyMessage, response.getBody().getMessage());
	}

	// ==================== handleBadRequest() Tests ====================

	@Test
	void shouldReturn400WhenEmptyCartExceptionIsThrown() {
		// Given
		EmptyCartException exception = new EmptyCartException(EMPTY_CART_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleBadRequest(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(EMPTY_CART_MESSAGE, response.getBody().getMessage());
	}

	@Test
	void shouldReturn400WhenIllegalArgumentExceptionIsThrown() {
		// Given
		IllegalArgumentException exception = new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleBadRequest(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(ILLEGAL_ARGUMENT_MESSAGE, response.getBody().getMessage());
	}


	@Test
	void shouldReturnCorrectMessageWhenBadRequestExceptionHasNullMessage() {
		// Given
		IllegalArgumentException exception = new IllegalArgumentException((String) null);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleBadRequest(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertNull(response.getBody().getMessage());
	}

	@Test
	void shouldReturnCorrectMessageWhenBadRequestExceptionHasEmptyMessage() {
		// Given
		String emptyMessage = "";
		EmptyCartException exception = new EmptyCartException(emptyMessage);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleBadRequest(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(emptyMessage, response.getBody().getMessage());
	}

	// ==================== handleImageInitializationException() Tests ====================

	@Test
	void shouldReturn500WhenImageInitializationExceptionIsThrown() {
		// Given
		ImageInitializationException exception = new ImageInitializationException(IMAGE_INIT_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleImageInitializationException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(IMAGE_INIT_MESSAGE, response.getBody().getMessage());
	}


	@Test
	void shouldReturnCorrectMessageWhenImageInitializationExceptionHasNullMessage() {
		// Given
		ImageInitializationException exception = new ImageInitializationException(null);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleImageInitializationException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertNull(response.getBody().getMessage());
	}

	@Test
	void shouldReturnCorrectMessageWhenImageInitializationExceptionHasEmptyMessage() {
		// Given
		String emptyMessage = "";
		ImageInitializationException exception = new ImageInitializationException(emptyMessage);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleImageInitializationException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(emptyMessage, response.getBody().getMessage());
	}

	// ==================== handleValidationErrors() Tests ====================

	@Test
	void shouldReturn400WhenMethodArgumentNotValidExceptionIsThrown() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError fieldError = new FieldError("objectName", "fieldName", VALIDATION_ERROR_MESSAGE);
		when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleValidationErrors(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(VALIDATION_ERROR_MESSAGE, response.getBody().getMessage());
	}

	@Test
	void shouldReturnDefaultMessageWhenNoFieldErrorsPresent() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleValidationErrors(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Validation failed", response.getBody().getMessage());
	}

	@Test
	void shouldReturnFirstFieldErrorWhenMultipleFieldErrorsPresent() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError firstError = new FieldError("objectName", "firstName", "First name is required");
		FieldError secondError = new FieldError("objectName", "lastName", "Last name is required");
		when(bindingResult.getFieldErrors()).thenReturn(List.of(firstError, secondError));

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleValidationErrors(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("First name is required", response.getBody().getMessage());
	}



	@Test
	void shouldHandleFieldErrorWithNullDefaultMessage() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError fieldError = new FieldError("objectName", "fieldName", null);
		when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleValidationErrors(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		// When field error has null message, the orElse should return "Validation failed"
		assertEquals("Validation failed", response.getBody().getMessage());
	}

	// ==================== handleGenericException() Tests ====================

	@Test
	void shouldReturn500WhenGenericExceptionIsThrown() {
		// Given
		Exception exception = new Exception(GENERIC_ERROR_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("An unexpected error occurred: " + GENERIC_ERROR_MESSAGE, response.getBody().getMessage());
	}


	@Test
	void shouldHandleGenericExceptionWithNullMessage() {
		// Given
		Exception exception = new Exception((String) null);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("An unexpected error occurred: null", response.getBody().getMessage());
	}

	@Test
	void shouldHandleGenericExceptionWithEmptyMessage() {
		// Given
		String emptyMessage = "";
		Exception exception = new Exception(emptyMessage);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("An unexpected error occurred: " + emptyMessage, response.getBody().getMessage());
	}

	@Test
	void shouldHandleRuntimeExceptionAsGenericException() {
		// Given
		RuntimeException exception = new RuntimeException("Unexpected runtime error");

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("An unexpected error occurred: Unexpected runtime error", response.getBody().getMessage());
	}

	@Test
	void shouldHandleNullPointerExceptionAsGenericException() {
		// Given
		NullPointerException exception = new NullPointerException("Null value encountered");

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("An unexpected error occurred: Null value encountered", response.getBody().getMessage());
	}

	// ==================== Edge Cases and Additional Coverage ====================

	@Test
	void shouldHandleExceptionWithVeryLongMessage() {
		// Given
		String longMessage = "Error: " + "x".repeat(1000);
		ItemNotFoundException exception = new ItemNotFoundException(longMessage);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(longMessage, response.getBody().getMessage());
		assertEquals(1007, response.getBody().getMessage().length());
	}

	@Test
	void shouldHandleExceptionWithSpecialCharacters() {
		// Given
		String specialMessage = "Error with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
		EmptyCartException exception = new EmptyCartException(specialMessage);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleBadRequest(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(specialMessage, response.getBody().getMessage());
	}

	@Test
	void shouldHandleExceptionWithUnicodeCharacters() {
		// Given
		String unicodeMessage = "Error: товар не найден 商品が見つかりません 🚫";
		OrderNotFoundException exception = new OrderNotFoundException(unicodeMessage);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(unicodeMessage, response.getBody().getMessage());
	}

	@Test
	void shouldHandleExceptionWithNewlineCharacters() {
		// Given
		String messageWithNewlines = "Error on line 1\nError on line 2\nError on line 3";
		ImageInitializationException exception = new ImageInitializationException(messageWithNewlines);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleImageInitializationException(exception);

		// Then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(messageWithNewlines, response.getBody().getMessage());
	}

	@Test
	void shouldVerifyApiErrorResponseStructure() {
		// Given
		ItemNotFoundException exception = new ItemNotFoundException(ITEM_NOT_FOUND_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

		// Then
		assertNotNull(response);
		assertNotNull(response.getBody());
		ApiErrorResponse errorResponse = response.getBody();

		// Verify that ApiErrorResponse has the expected structure
		assertNotNull(errorResponse.getMessage());
		assertEquals(ITEM_NOT_FOUND_MESSAGE, errorResponse.getMessage());
	}

	@Test
	void shouldEnsureResponseEntityHasCorrectGenericType() {
		// Given
		EmptyCartException exception = new EmptyCartException(EMPTY_CART_MESSAGE);

		// When
		ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleBadRequest(exception);

		// Then
		assertNotNull(response);
		assertTrue(response.hasBody());
		assertInstanceOf(ApiErrorResponse.class, response.getBody());
	}
}
