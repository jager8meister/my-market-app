package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import ru.yandex.practicum.mymarket.enums.CartAction;

@Data
@Schema(description = "Parameters for changing item count on catalog page")
public class ChangeItemCountRequestDto {

	@NotNull(message = "Item ID is required")
	@Schema(
			description = "Item identifier whose count must be changed.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1"
	)
	private Long id;

	@Schema(
			description = "Search string from GET /items used to build redirect URL.",
			example = "smartphone"
	)
	private String search = "";

	@Schema(
			description = "Sort mode from GET /items.",
			example = "NO"
	)
	private ru.yandex.practicum.mymarket.enums.SortType sort = ru.yandex.practicum.mymarket.enums.SortType.NO;

	@NotNull(message = "Action is required")
	@Schema(
			description = "Action: PLUS — increase count by one, MINUS — decrease count by one.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "PLUS"
	)
	private CartAction action;

	@Schema(
			description = "Page number from GET /items (1-based, first page is 1).",
			example = "1"
	)
	private Integer pageNumber = 1;

	@Schema(
			description = "Page size from GET /items.",
			example = "5"
	)
	private Integer pageSize = 5;
}
