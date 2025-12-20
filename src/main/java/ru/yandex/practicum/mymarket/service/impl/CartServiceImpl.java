package ru.yandex.practicum.mymarket.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.WebSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.mapper.CartMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

	private static final String CART_KEY = "cart";

	private final ItemRepository itemRepository;
	private final CartMapper cartMapper;

	@Override
	public Mono<Void> applyCartAction(CartAction action, Long itemId, WebSession session) {
		if (action == null || itemId == null) {
			return Mono.empty();
		}
		return switch (action) {
			case PLUS -> addItem(itemId, session);
			case MINUS -> removeOne(itemId, session);
			case DELETE -> removeAll(itemId, session);
		};
	}

	@Override
	public Mono<Void> clear(WebSession session) {
		session.getAttributes().remove(CART_KEY);
		return Mono.empty();
	}

	@Override
	@Transactional(readOnly = true)
	public Flux<CartEntry> getItems(WebSession session) {
		return getCartMap(session)
				.flatMapMany(cart -> cart.isEmpty()
						? Flux.empty()
						: itemRepository.findAllById(cart.keySet())
						.flatMap(item -> toEntry(cart, item)));
	}

	private Mono<CartEntry> toEntry(Map<Long, Integer> cart, ItemEntity item) {
		Integer count = cart.get(item.getId());
		if (count == null || count <= 0) {
			return Mono.empty();
		}
		return Mono.just(new CartEntry(item, count));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<Long> getTotalPrice(WebSession session) {
		return getItems(session)
				.map(entry -> entry.getItem().getPrice() * entry.getCount())
				.reduce(0L, Long::sum);
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<CartStateResponseDto> getCart(WebSession session) {
		return getItems(session)
				.map(cartMapper::toCartItemResponse)
				.collectList()
				.zipWith(getTotalPrice(session), (items, total) -> new CartStateResponseDto(items, total));
	}

	@Override
	public Mono<CartStateResponseDto> updateCart(CartUpdateRequestDto request, WebSession session) {
		return applyCartAction(request.action(), request.id(), session).then(getCart(session));
	}

	private Mono<Void> addItem(Long itemId, WebSession session) {
		log.debug("addItem called with itemId: {}", itemId);
		return getCartMap(session)
				.zipWith(itemRepository.findById(itemId), (cart, item) -> {
					int currentCount = cart.getOrDefault(itemId, 0);
					cart.put(itemId, currentCount + 1);
					return cart;
				})
				.then();
	}

	private Mono<Void> removeOne(Long itemId, WebSession session) {
		log.debug("removeOne called with itemId: {}", itemId);
		return getCartMap(session)
				.doOnNext(cart -> cart.computeIfPresent(itemId, (id, count) -> count > 1 ? count - 1 : null))
				.then();
	}

	private Mono<Void> removeAll(Long itemId, WebSession session) {
		log.debug("removeAll called with itemId: {}", itemId);
		return getCartMap(session).doOnNext(cart -> cart.remove(itemId)).then();
	}

	@Override
	public Mono<Integer> getItemCountInCart(Long itemId, WebSession session) {
		log.debug("getItemCountInCart called with itemId: {}", itemId);
		return getCartMap(session)
				.map(cart -> cart.getOrDefault(itemId, 0));
	}

	private Mono<Map<Long, Integer>> getCartMap(WebSession session) {
		Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttributes()
			.computeIfAbsent(CART_KEY, k -> new ConcurrentHashMap<>());
		return Mono.just(cart);
	}
}
