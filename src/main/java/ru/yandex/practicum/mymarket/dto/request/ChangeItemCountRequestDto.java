package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.mymarket.enums.CartAction;

@Schema(description = "Parameters for changing item count")
public record ChangeItemCountRequestDto(
	@NotNull(message = "Item ID is required")
	@Schema(
			description = "Item identifier whose count must be changed.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1"
	)
	Long id,

	@NotNull(message = "Action is required")
	@Schema(
			description = "Action: PLUS — increase count by one, MINUS — decrease count by one.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "PLUS"
	)
	CartAction action
) {}
