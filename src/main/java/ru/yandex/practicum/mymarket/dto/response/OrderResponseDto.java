package ru.yandex.practicum.mymarket.dto.response;

import java.util.List;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order information with items and total sum")
public record OrderResponseDto(
	@Schema(description = "Order identifier", example = "1")
	long id,

	@Schema(description = "Items included in the order")
	List<OrderItemResponseDto> items,

	@Schema(description = "Total order sum", example = "39980")
	long totalSum,

	@Schema(description = "Order creation time")
	LocalDateTime createdAt
) {}
