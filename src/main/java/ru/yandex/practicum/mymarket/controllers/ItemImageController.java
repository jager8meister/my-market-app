package ru.yandex.practicum.mymarket.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.mymarket.service.ItemService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Item image", description = "Download item image")
public class ItemImageController {

	private final ItemService itemService;

	@GetMapping("/items/{id}/image")
	@Operation(summary = "Get item image by identifier")
	public ResponseEntity<byte[]> getItemImage(
			@Parameter(description = "Item identifier", required = true)
			@PathVariable("id") Long id) {
		return itemService.getItemImageResponse(id);
	}
}
