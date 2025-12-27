package ru.yandex.practicum.mymarket.service.impl;

import java.time.LocalDateTime;

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
import ru.yandex.practicum.mymarket.entity.CartItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.mapper.CartMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.UserService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

	private final CartItemRepository cartItemRepository;
	private final ItemRepository itemRepository;
	private final CartMapper cartMapper;
	private final ItemService itemService;
	private final UserService userService;

	@Override
	public Mono<Void> applyCartAction(CartAction action, Long itemId, WebSession session) {
		log.debug("applyCartAction called with action: {}, itemId: {}", action, itemId);
		if (action == null || itemId == null) {
			log.warn("applyCartAction called with null action or itemId: action={}, itemId={}", action, itemId);
			return Mono.empty();
		}
		return switch (action) {
			case PLUS -> addItem(itemId);
			case MINUS -> removeOne(itemId);
			case DELETE -> removeAll(itemId);
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
		return userService.getCurrentUserId()
				.flatMap(userId -> cartItemRepository.deleteByUserId(userId)
						.doOnSuccess(v -> log.debug("Cart cleared successfully for user {}", userId)));
	}

	@Override
	@Transactional(readOnly = true)
	public Flux<CartEntry> getItems(WebSession session) {
		log.debug("getItems called");
		return userService.getCurrentUserId()
				.flatMapMany(userId -> {
					log.debug("Getting cart items for user {}", userId);
					return cartItemRepository.findByUserId(userId);
				})
				.flatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
						.map(item -> new CartEntry(item, cartItem.getCount())))
				.doOnComplete(() -> log.debug("getItems completed"));
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
		return userService.getCurrentUserId()
				.flatMapMany(userId -> {
					log.debug("Getting cart for user {}", userId);
					return cartItemRepository.findByUserId(userId);
				})
				.flatMap(cartItem -> itemService.getItem(cartItem.getItemId())
						.map(item -> new CartItemResponseDto(
								item.id(),
								item.title(),
								item.description(),
								item.imgPath(),
								item.price(),
								cartItem.getCount()
						))
						.doOnSuccess(i -> log.debug("Loaded item {} from cache for cart", cartItem.getItemId())))
				.collectList()
				.map(items -> {
					long total = items.stream()
							.mapToLong(item -> item.price() * item.count())
							.sum();
					return new CartStateResponseDto(items, total);
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

	private Mono<Void> addItem(Long itemId) {
		log.debug("addItem called with itemId: {}", itemId);
		return userService.getCurrentUserId()
				.flatMap(userId -> cartItemRepository.findByUserIdAndItemId(userId, itemId)
						.flatMap(existing -> {
							existing.setCount(existing.getCount() + 1);
							existing.setUpdatedAt(LocalDateTime.now());
							log.debug("Incrementing count for user {}, item {}: {} -> {}",
									userId, itemId, existing.getCount() - 1, existing.getCount());
							return cartItemRepository.save(existing);
						})
						.switchIfEmpty(Mono.defer(() -> {
							CartItemEntity newItem = new CartItemEntity();
							newItem.setUserId(userId);
							newItem.setItemId(itemId);
							newItem.setCount(1);
							newItem.setCreatedAt(LocalDateTime.now());
							newItem.setUpdatedAt(LocalDateTime.now());
							log.debug("Adding new item to cart for user {}, item {}", userId, itemId);
							return cartItemRepository.save(newItem);
						})))
				.then()
				.doOnSuccess(v -> log.debug("Item {} added to cart successfully", itemId));
	}

	private Mono<Void> removeOne(Long itemId) {
		log.debug("removeOne called with itemId: {}", itemId);
		return userService.getCurrentUserId()
				.flatMap(userId -> cartItemRepository.findByUserIdAndItemId(userId, itemId)
						.flatMap(existing -> {
							if (existing.getCount() > 1) {
								int oldCount = existing.getCount();
								existing.setCount(oldCount - 1);
								existing.setUpdatedAt(LocalDateTime.now());
								log.debug("Decrementing count for user {}, item {}: {} -> {}",
										userId, itemId, oldCount, existing.getCount());
								return cartItemRepository.save(existing).then();
							} else {
								log.debug("Removing item {} from cart for user {}", itemId, userId);
								return cartItemRepository.deleteByUserIdAndItemId(userId, itemId);
							}
						}))
				.then()
				.doOnSuccess(v -> log.debug("Removed one item {} from cart", itemId));
	}

	private Mono<Void> removeAll(Long itemId) {
		log.debug("removeAll called with itemId: {}", itemId);
		return userService.getCurrentUserId()
				.flatMap(userId -> cartItemRepository.deleteByUserIdAndItemId(userId, itemId)
						.doOnSuccess(v -> log.debug("Removed all items {} from cart for user {}", itemId, userId)))
				.then();
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<Integer> getItemCountInCart(Long itemId, WebSession session) {
		log.debug("getItemCountInCart called with itemId: {}", itemId);
		return userService.getCurrentUserId()
				.flatMap(userId -> cartItemRepository.findByUserIdAndItemId(userId, itemId)
						.map(CartItemEntity::getCount)
						.defaultIfEmpty(0))
				.doOnSuccess(count -> log.debug("Item {} count in cart: {}", itemId, count));
	}
}
