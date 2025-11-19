package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Single item entry in order")
public class OrderItemResponseDto {

	@Schema(description = "Item title", example = "Smartphone A1")
	private String title;

	@Schema(description = "Price of one item", example = "19990")
	private long price;

	@Schema(description = "Number of items", example = "2")
	private int count;
}
