package ru.yandex.practicum.mymarket.service;

import java.util.List;

import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

public interface CartService {

	void addItem(Long itemId);

	void removeOne(Long itemId);

	void removeAll(Long itemId);

	List<CartEntry> getItems();

	long getTotalPrice();

	void clear();

	String showCart(Model model);

	String updateCart(CartUpdateRequestDto request);

	String changeItemCount(ChangeItemCountRequestDto request);

	String changeItemCountOnDetails(Long id, CartAction action);
}
