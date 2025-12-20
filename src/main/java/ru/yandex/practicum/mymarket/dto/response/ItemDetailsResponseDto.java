package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item information for item details page")
public record ItemDetailsResponseDto(
	@Schema(description = "Item identifier", example = "1")
	Long id,

	@Schema(description = "Item title", example = "Smartphone A1")
	String title,

	@Schema(description = "Item full description", example = "Detailed description of item characteristics")
	String description,

	@Schema(description = "Item image path", example = "/images/ball.jpg")
	String imgPath,

	@Schema(description = "Item price", example = "19990")
	long price,

	@Schema(description = "Number of items of this type in cart", example = "1")
	int count
) {}
