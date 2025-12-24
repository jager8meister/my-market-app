package ru.yandex.practicum.mymarket.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.enums.SortType;

public record CartActionWithNavigationDto(
	@NotNull Long id,
	@NotNull CartAction action,
	String search,
	SortType sort,
	@Positive Integer pageNumber,
	@Positive Integer pageSize
) {}
