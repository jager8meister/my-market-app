package ru.yandex.practicum.mymarket.service;

import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;

public interface ItemService {

	Flux<ItemResponseDto> getItems(ItemsFilterRequestDto request);

	Mono<ItemDetailsResponseDto> getItem(Long id);

	Mono<ResponseEntity<byte[]>> getItemImageResponse(Long id);
}
