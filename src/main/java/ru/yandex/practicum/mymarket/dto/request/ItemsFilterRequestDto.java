package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
	SortType sort
) {
	public ItemsFilterRequestDto {
		if (search == null) search = "";
		if (sort == null) sort = SortType.NO;
	}
}
