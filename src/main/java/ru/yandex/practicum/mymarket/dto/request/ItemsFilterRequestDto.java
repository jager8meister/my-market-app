package ru.yandex.practicum.mymarket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.mymarket.enums.SortType;

@Data
@Schema(description = "Filter and sort parameters for items list")
public class ItemsFilterRequestDto {

	@Schema(
			description = "Search string for item title and description. "
					+ "If empty or not provided all items are shown.",
			example = "smartphone"
	)
	private String search = "";

	@NotNull(message = "Sort type is required")
	@Schema(
			description = "Sort mode: NO — no sorting, ALPHA — by title, PRICE — by price.",
			example = "NO"
	)
	private SortType sort = SortType.NO;

	@NotNull(message = "Page number is required")
	@Min(value = 1, message = "Page number must be at least 1")
	@Schema(
			description = "Page number (1-based, first page is 1)",
			example = "1"
	)
	private Integer pageNumber = 1;

	@NotNull(message = "Page size is required")
	@Min(value = 1, message = "Page size must be at least 1")
	@Schema(
			description = "Page size - number of items per page",
			example = "5"
	)
	private Integer pageSize = 5;
}
