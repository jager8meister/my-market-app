package ru.yandex.practicum.mymarket.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
@Tag(name = "Items", description = "Items API")
public class ApiItemsController {

	private final ItemService itemService;
	private final CartService cartService;

	@GetMapping({"", "items"})
	@Operation(summary = "Get items list")
	public Mono<Page<ItemResponseDto>> getItems(
			@Valid @ModelAttribute ItemsFilterRequestDto filter,
			@RequestParam(defaultValue = "1") @Positive int pageNumber,
			@RequestParam(defaultValue = "5") @Positive int pageSize) {
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
		return itemService.getItems(filter, pageable);
	}

	@PostMapping("/items")
	@Operation(summary = "Change item count from catalog page")
	public Mono<CartStateResponseDto> changeItemCount(@Valid @ModelAttribute ChangeItemCountRequestDto request, WebSession session) {
		return cartService.applyActionAndGetCart(request.action(), request.id(), session);
	}

	@GetMapping("items/{id}")
	@Operation(summary = "Get item details")
	public Mono<ItemDetailsResponseDto> getItem(@PathVariable("id") @Positive Long id) {
		return itemService.getItem(id);
	}

	@PostMapping("items/{id}")
	@Operation(summary = "Change item count from item details page")
	public Mono<CartStateResponseDto> changeItemCountOnDetails(
			@PathVariable("id") @Positive Long id,
			@Valid @ModelAttribute ChangeItemCountRequestDto request,
			WebSession session) {
		return cartService.applyActionAndGetCart(request.action(), id, session);
	}

	@GetMapping("items/{id}/image")
	@Operation(summary = "Get item image")
	public Mono<ResponseEntity<byte[]>> getItemImage(@PathVariable("id") @Positive Long id) {
		return itemService.getItemImageResponse(id);
	}
}
