package ru.yandex.practicum.mymarket.dto.model;

public record CommonViewAttributes(
	String balance,
	String username,
	boolean isAuthenticated
) {
}
