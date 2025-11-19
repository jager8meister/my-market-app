package ru.yandex.practicum.mymarket.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
@RequiredArgsConstructor
@Tag(name = "Items", description = "Catalog page with items and cart actions")
public class ItemsController {

	private final ItemService itemService;
	private final CartService cartService;

	@GetMapping({"/", "/items"})
	@Operation(
			summary = "Show catalog page",
			description = "Returns items.html with items list using filter, sort and paging parameters."
	)
	public String showItems(@Valid ItemsFilterRequestDto request, Model model) {
		return itemService.showItems(request, model);
	}

	@PostMapping("/items")
	@Operation(
			summary = "Change item count from catalog page",
			description = "Increases or decreases item count in cart and redirects back to /items."
	)
	public String changeItemCount(@Valid ChangeItemCountRequestDto request) {
		return cartService.changeItemCount(request);
	}
}
