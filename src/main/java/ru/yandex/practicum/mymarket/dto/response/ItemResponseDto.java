package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item information for catalog view")
public record ItemResponseDto(
	@Schema(description = "Item identifier", example = "1")
	Long id,

	@Schema(description = "Item title", example = "Smartphone A1")
	String title,

	@Schema(description = "Item short description", example = "Smartphone with 6.1\" display")
	String description,

	@Schema(description = "Item description path", example = "/images/ball.jpg")
	String imgPath,

	@Schema(description = "Item price", example = "19990")
	long price,

	@Schema(description = "Number of items of this type in cart", example = "2")
	int count
) {}
