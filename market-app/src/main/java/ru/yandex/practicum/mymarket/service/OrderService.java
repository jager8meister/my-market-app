package ru.yandex.practicum.mymarket.service;

import org.springframework.web.server.WebSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;

public interface OrderService {

	Mono<OrderResponseDto> buy(WebSession session);

	Flux<OrderResponseDto> getOrders();

	Mono<OrderResponseDto> getOrder(long id);
}
