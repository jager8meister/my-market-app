package ru.yandex.practicum.mymarket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error message returned by the API")
public class ApiErrorResponse {

	@Schema(description = "Human readable error message", example = "Item not found with id: 999")
	private String message;
}
