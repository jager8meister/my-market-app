package ru.yandex.practicum.mymarket.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CachedItemsPageDto(
	List<ItemResponseDto> content,
	int pageNumber,
	int pageSize,
	long totalElements
) {
	@JsonCreator
	public CachedItemsPageDto(
			@JsonProperty("content") List<ItemResponseDto> content,
			@JsonProperty("pageNumber") int pageNumber,
			@JsonProperty("pageSize") int pageSize,
			@JsonProperty("totalElements") long totalElements) {
		this.content = content;
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
		this.totalElements = totalElements;
	}
}
