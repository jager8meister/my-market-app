package ru.yandex.practicum.mymarket.dto.response;

import java.util.List;

public record CartStateResponseDto(
	List<CartItemResponseDto> items,
	long total
) {}
