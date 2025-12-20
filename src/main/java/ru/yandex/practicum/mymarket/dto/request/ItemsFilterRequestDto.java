package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.mymarket.enums.SortType;

@Schema(description = "Filter and sort parameters for items list")
public record ItemsFilterRequestDto(
	@Schema(
			description = "Search string for item title and description. "
					+ "If empty or not provided all items are shown.",
			example = "smartphone"
	)
	String search,

	@NotNull(message = "Sort type is required")
	@Schema(
			description = "Sort mode: NO — no sorting, ALPHA — by title, PRICE — by price.",
			example = "NO"
	)
	SortType sort,

	@NotNull(message = "Page number is required")
	@Min(value = 1, message = "Page number must be at least 1")
	@Schema(
			description = "Page number (1-based, first page is 1)",
			example = "1"
	)
	Integer pageNumber,

	@NotNull(message = "Page size is required")
	@Min(value = 1, message = "Page size must be at least 1")
	@Schema(
			description = "Page size - number of items per page",
			example = "5"
	)
	Integer pageSize
) {
	public ItemsFilterRequestDto {
		if (search == null) search = "";
		if (sort == null) sort = SortType.NO;
		if (pageNumber == null) pageNumber = 1;
		if (pageSize == null) pageSize = 5;
	}
}
