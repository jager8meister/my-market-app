package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paging information")
public class PagingDto {

	@Schema(description = "Page size - number of items per page", example = "5")
	private int pageSize;

	@Schema(description = "Current page number (1-based)", example = "1")
	private int pageNumber;

	@Schema(description = "True if not the first page", example = "false")
	private boolean hasPrevious;

	@Schema(description = "True if not the last page", example = "true")
	private boolean hasNext;
}
