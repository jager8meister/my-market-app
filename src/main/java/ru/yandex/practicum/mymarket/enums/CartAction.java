package ru.yandex.practicum.mymarket.enums;

import ru.yandex.practicum.mymarket.service.CartService;

public enum CartAction {
	PLUS,
	MINUS,
	DELETE;

	public void execute(CartService cartService, Long itemId) {
		switch (this) {
			case PLUS -> cartService.addItem(itemId);
			case MINUS -> cartService.removeOne(itemId);
			case DELETE -> cartService.removeAll(itemId);
		}
	}
}
