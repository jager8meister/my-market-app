package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Single item entry in order")
public record OrderItemResponseDto(
	@Schema(description = "Item title", example = "Smartphone A1")
	String title,

	@Schema(description = "Price of one item", example = "19990")
	long price,

	@Schema(description = "Number of items", example = "2")
	int count
) {}
