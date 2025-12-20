package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.enums.SortType;

@Schema(description = "Parameters for changing item count on catalog page")
public record ChangeItemCountRequestDto(
	@NotNull(message = "Item ID is required")
	@Schema(
			description = "Item identifier whose count must be changed.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1"
	)
	Long id,

	@Schema(
			description = "Search string from GET /items used to build redirect URL.",
			example = "smartphone"
	)
	String search,

	@Schema(
			description = "Sort mode from GET /items.",
			example = "NO"
	)
	SortType sort,

	@NotNull(message = "Action is required")
	@Schema(
			description = "Action: PLUS — increase count by one, MINUS — decrease count by one.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "PLUS"
	)
	CartAction action,

	@Schema(
			description = "Page number from GET /items (1-based, first page is 1).",
			example = "1"
	)
	Integer pageNumber,

	@Schema(
			description = "Page size from GET /items.",
			example = "5"
	)
	Integer pageSize
) {
	public ChangeItemCountRequestDto {
		if (search == null) search = "";
		if (sort == null) sort = SortType.NO;
		if (pageNumber == null) pageNumber = 1;
		if (pageSize == null) pageSize = 5;
	}
}
