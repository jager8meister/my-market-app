package ru.yandex.practicum.mymarket.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order information with items and total sum")
public class OrderResponseDto {

	@Schema(description = "Order identifier", example = "1")
	private long id;

	@Schema(description = "Items included in the order")
	private List<OrderItemResponseDto> items;

	@Schema(description = "Total order sum", example = "39980")
	private long totalSum;
}
