package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item information for item details page")
public class ItemDetailsResponseDto {

	@Schema(description = "Item identifier", example = "1")
	private Long id;

	@Schema(description = "Item title", example = "Smartphone A1")
	private String title;

	@Schema(description = "Item full description", example = "Detailed description of item characteristics")
	private String description;

	@Schema(description = "Item image path", example = "/images/ball.jpg")
	private String imgPath;

	@Schema(description = "Item price", example = "19990")
	private long price;

	@Schema(description = "Number of items of this type in cart", example = "1")
	private int count;
}
