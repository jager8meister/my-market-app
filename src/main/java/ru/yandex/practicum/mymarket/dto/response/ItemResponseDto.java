package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item information for catalog view")
public class ItemResponseDto {

	@Schema(description = "Item identifier", example = "1")
	private Long id;

	@Schema(description = "Item title", example = "Smartphone A1")
	private String title;

	@Schema(description = "Item short description", example = "Smartphone with 6.1\" display")
	private String description;

	@Schema(description = "Item description path", example = "/images/ball.jpg")
	private String imgPath;

	@Schema(description = "Item price", example = "19990")
	private long price;

	@Schema(description = "Number of items of this type in cart", example = "2")
	private int count;
}
