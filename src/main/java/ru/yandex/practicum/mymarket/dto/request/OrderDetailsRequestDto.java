package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Parameters for fetching order details")
public class OrderDetailsRequestDto {

	@Min(value = 1, message = "Order ID must be positive")
	@Schema(description = "Order identifier", example = "1")
	private long id;

	@Schema(description = "Flag that order has just been created", example = "true")
	private boolean newOrder;
}
