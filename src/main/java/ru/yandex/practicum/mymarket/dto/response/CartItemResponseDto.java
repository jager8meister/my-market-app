package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item information in shopping cart")
public class CartItemResponseDto {

	@Schema(description = "Item identifier", example = "1")
	private Long id;

	@Schema(description = "Item title", example = "Smartphone A1")
	private String title;

	@Schema(description = "Item short description", example = "Short description used on cart page")
	private String description;

	@Schema(description = "Image path (debug purposes)", example = "images/phone.svg")
	private String imgPath;

	@Schema(description = "Item price", example = "19990")
	private long price;

	@Schema(description = "Number of items of this type in cart", example = "3")
	private int count;
}
