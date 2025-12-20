package ru.yandex.practicum.mymarket.service;

import org.springframework.web.server.WebSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

public interface CartService {

	Mono<Void> applyCartAction(CartAction action, Long itemId, WebSession session);

	Mono<Void> clear(WebSession session);

	Flux<CartEntry> getItems(WebSession session);

	Mono<Long> getTotalPrice(WebSession session);

	Mono<CartStateResponseDto> getCart(WebSession session);

	Mono<CartStateResponseDto> updateCart(CartUpdateRequestDto request, WebSession session);

	Mono<Integer> getItemCountInCart(Long itemId, WebSession session);
}
