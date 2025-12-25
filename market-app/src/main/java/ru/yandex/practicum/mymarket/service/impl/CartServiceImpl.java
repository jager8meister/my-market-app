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
import ru.yandex.practicum.mymarket.dto.request.CartActionWithNavigationDto;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.mapper.CartMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

	private static final String CART_KEY = "cart";

	private final ItemRepository itemRepository;
	private final CartMapper cartMapper;
	private final ItemService itemService;

	@Override
	public Mono<Void> applyCartAction(CartAction action, Long itemId, WebSession session) {
		log.debug("applyCartAction called with action: {}, itemId: {}", action, itemId);
		if (action == null || itemId == null) {
			log.warn("applyCartAction called with null action or itemId: action={}, itemId={}", action, itemId);
			return Mono.empty();
		}
		return switch (action) {
			case PLUS -> addItem(itemId, session);
			case MINUS -> removeOne(itemId, session);
			case DELETE -> removeAll(itemId, session);
		};
	}

	@Override
	public Mono<String> applyCartActionAndBuildRedirect(CartActionWithNavigationDto params, WebSession session) {
		log.debug("applyCartActionAndBuildRedirect called with action: {}, itemId: {}",
				params.action(), params.id());
		return applyCartAction(params.action(), params.id(), session)
				.thenReturn(buildRedirectUrl(params))
				.doOnSuccess(url -> log.debug("Redirecting to: {}", url));
	}

	private String buildRedirectUrl(CartActionWithNavigationDto params) {
		StringBuilder url = new StringBuilder("redirect:/items?pageNumber=")
				.append(params.pageNumber() != null ? params.pageNumber() : 1)
				.append("&pageSize=")
				.append(params.pageSize() != null ? params.pageSize() : 5);

		if (params.sort() != null) {
			url.append("&sort=").append(params.sort());
		}
		if (params.search() != null && !params.search().isBlank()) {
			url.append("&search=").append(params.search());
		}

		return url.toString();
	}

	@Override
	public Mono<Void> clear(WebSession session) {
		log.debug("clear called - clearing cart");
		return Mono.fromRunnable(() -> session.getAttributes().remove(CART_KEY))
				.doOnSuccess(v -> log.debug("Cart cleared successfully"))
				.then();
	}

	@Override
	@Transactional(readOnly = true)
	public Flux<CartEntry> getItems(WebSession session) {
		log.debug("getItems called");
		return getCartMap(session)
				.flatMapMany(cart -> {
					log.debug("Cart contains {} unique items", cart.size());
					return cart.isEmpty()
							? Flux.empty()
							: itemRepository.findAllById(cart.keySet())
							.flatMap(item -> toEntry(cart, item));
				})
				.doOnComplete(() -> log.debug("getItems completed"));
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
		log.debug("getTotalPrice called");
		return getItems(session)
				.map(entry -> entry.getItem().getPrice() * entry.getCount())
				.reduce(0L, Long::sum)
				.doOnSuccess(total -> log.debug("Total price calculated: {}", total));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<CartStateResponseDto> getCart(WebSession session) {
		log.debug("getCart called");
		return getCartMap(session)
				.flatMap(cartMap -> {
					if (cartMap.isEmpty()) {
						log.debug("Cart is empty");
						return Mono.just(new CartStateResponseDto(java.util.List.of(), 0L));
					}
					return Flux.fromIterable(cartMap.entrySet())
							.flatMap(entry -> {
								Long itemId = entry.getKey();
								Integer count = entry.getValue();
								return itemService.getItem(itemId)
										.map(item -> new CartItemResponseDto(
												item.id(),
												item.title(),
												item.description(),
												item.imgPath(),
												item.price(),
												count
										))
										.doOnSuccess(cartItem -> log.debug("Loaded item {} from cache for cart", itemId));
							})
							.collectList()
							.map(items -> {
								long total = items.stream()
										.mapToLong(item -> item.price() * item.count())
										.sum();
								return new CartStateResponseDto(items, total);
							});
				})
				.doOnSuccess(cart -> log.debug("getCart returned cart with {} items, total: {}",
						cart.items().size(), cart.total()));
	}

	@Override
	public Mono<CartStateResponseDto> updateCart(CartUpdateRequestDto request, WebSession session) {
		log.debug("updateCart called with action: {}, itemId: {}", request.action(), request.id());
		return applyActionAndGetCart(request.action(), request.id(), session);
	}

	@Override
	public Mono<CartStateResponseDto> applyActionAndGetCart(CartAction action, Long itemId, WebSession session) {
		log.debug("applyActionAndGetCart called with action: {}, itemId: {}", action, itemId);
		return applyCartAction(action, itemId, session)
				.then(getCart(session))
				.doOnSuccess(cart -> log.debug("Cart updated: {} items, total: {}",
						cart.items().size(), cart.total()));
	}

	private Mono<Void> addItem(Long itemId, WebSession session) {
		log.debug("addItem called with itemId: {}", itemId);
		return Mono.zip(getCartMap(session), itemRepository.findById(itemId))
				.doOnNext(tuple -> {
					Map<Long, Integer> cart = tuple.getT1();
					int currentCount = cart.getOrDefault(itemId, 0);
					int newCount = currentCount + 1;
					cart.put(itemId, newCount);
					log.debug("Item {} count updated: {} -> {}", itemId, currentCount, newCount);
				})
				.then()
				.doOnSuccess(v -> log.debug("Item {} added to cart successfully", itemId));
	}

	private Mono<Void> removeOne(Long itemId, WebSession session) {
		log.debug("removeOne called with itemId: {}", itemId);
		return getCartMap(session)
				.doOnNext(cart -> {
					Integer oldCount = cart.get(itemId);
					cart.computeIfPresent(itemId, (id, count) -> count > 1 ? count - 1 : null);
					Integer newCount = cart.get(itemId);
					if (oldCount != null) {
						log.debug("Item {} count updated: {} -> {}", itemId, oldCount, newCount != null ? newCount : 0);
					}
				})
				.then()
				.doOnSuccess(v -> log.debug("Removed one item {} from cart", itemId));
	}

	private Mono<Void> removeAll(Long itemId, WebSession session) {
		log.debug("removeAll called with itemId: {}", itemId);
		return getCartMap(session)
				.doOnNext(cart -> {
					Integer removed = cart.remove(itemId);
					if (removed != null) {
						log.debug("Removed all items {} from cart (count was: {})", itemId, removed);
					} else {
						log.debug("Item {} was not in cart", itemId);
					}
				})
				.then();
	}

	@Override
	public Mono<Integer> getItemCountInCart(Long itemId, WebSession session) {
		log.debug("getItemCountInCart called with itemId: {}", itemId);
		return getCartMap(session)
				.map(cart -> cart.getOrDefault(itemId, 0))
				.doOnSuccess(count -> log.debug("Item {} count in cart: {}", itemId, count));
	}

	private Mono<Map<Long, Integer>> getCartMap(WebSession session) {
		return Mono.fromCallable(() ->
			(Map<Long, Integer>) session.getAttributes()
				.computeIfAbsent(CART_KEY, k -> new ConcurrentHashMap<>())
		);
	}
}
