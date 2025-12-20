package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error message returned by the API")
public record ApiErrorResponse(
	@Schema(description = "Human readable error message", example = "Item not found with id: 999")
	String message
) {}
