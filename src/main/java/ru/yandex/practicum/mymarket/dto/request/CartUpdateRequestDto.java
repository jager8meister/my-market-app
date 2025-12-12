package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.mymarket.enums.CartAction;

@Data
@Schema(description = "Parameters for changing item count on cart page")
public class CartUpdateRequestDto {

	@NotNull(message = "Item ID is required")
	@Schema(
			description = "Item identifier in cart.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1"
	)
	private Long id;

	@NotNull(message = "Action is required")
	@Schema(
			description = "Action: PLUS — increase count, MINUS — decrease count, DELETE — remove all items.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "PLUS"
	)
	private CartAction action;
}
