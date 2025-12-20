package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paging information")
public record PagingDto(
	@Schema(description = "Page size - number of items per page", example = "5")
	int pageSize,

	@Schema(description = "Current page number (1-based)", example = "1")
	int pageNumber,

	@Schema(description = "True if not the first page", example = "false")
	boolean hasPrevious,

	@Schema(description = "True if not the last page", example = "true")
	boolean hasNext
) {}
