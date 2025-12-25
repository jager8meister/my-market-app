package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item information in shopping cart")
public record CartItemResponseDto(
	@Schema(description = "Item identifier", example = "1")
	Long id,

	@Schema(description = "Item title", example = "Smartphone A1")
	String title,

	@Schema(description = "Item short description", example = "Short description used on cart page")
	String description,

	@Schema(description = "Image path (debug purposes)", example = "images/phone.svg")
	String imgPath,

	@Schema(description = "Item price", example = "19990")
	long price,

	@Schema(description = "Number of items of this type in cart", example = "3")
	int count
) {}
