package ru.yandex.practicum.mymarket.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
@RequiredArgsConstructor
@Tag(name = "Item details", description = "Single item details page and cart actions")
public class ItemDetailsController {

	private final ItemService itemService;
	private final CartService cartService;

	@GetMapping("/items/{id}")
	@Operation(summary = "Show item details page")
	public String showItem(
			@Parameter(description = "Item identifier", required = true)
			@PathVariable("id") Long id,
			Model model) {
		return itemService.showItemDetails(id, model);
	}

	@PostMapping("/items/{id}")
	@Operation(summary = "Change item count from item details page")
	public String changeItemCountOnDetails(
			@Parameter(description = "Item identifier", required = true)
			@PathVariable("id") Long id,
			@Parameter(description = "Action: PLUS or MINUS", required = true)
			@RequestParam("action") CartAction action) {
		return cartService.changeItemCountOnDetails(id, action);
	}
}
