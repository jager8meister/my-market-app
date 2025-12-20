package ru.yandex.practicum.mymarket.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.WebSession;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.enums.SortType;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequiredArgsConstructor
@Validated
public class ViewController {

	private final ItemService itemService;
	private final CartService cartService;
	private final OrderService orderService;

	@GetMapping(value = {"/", "/items"}, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> itemsPage(
			@ModelAttribute @Valid ItemsFilterRequestDto filter,
			@RequestParam(defaultValue = "1") @Positive int pageNumber,
			@RequestParam(defaultValue = "5") @Positive int pageSize,
			WebSession session) {
		SortType sort = filter.sort() == null ? SortType.NO : filter.sort();
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

		return cartService.getCart(session)
				.defaultIfEmpty(new CartStateResponseDto(Collections.emptyList(), 0L))
				.zipWith(itemService.getItems(filter, pageable))
				.flatMap(tuple -> {
					CartStateResponseDto cart = tuple.getT1();
					var page = tuple.getT2();
					Map<Long, Integer> cartCountMap = cart.items().stream()
							.collect(Collectors.toMap(CartItemResponseDto::id, CartItemResponseDto::count));
					return Flux.fromIterable(page.getContent())
							.map(item -> {
								int count = cartCountMap.getOrDefault(item.id(), 0);
								return new ItemResponseDto(
										item.id(),
										item.title(),
										item.description(),
										item.imgPath(),
										item.price(),
										count
								);
							})
							.collectList()
							.map(enrichedItems -> Rendering.view("items")
									.modelAttribute("items", enrichedItems)
									.modelAttribute("page", page)
									.modelAttribute("search", filter.search())
									.modelAttribute("sort", sort)
									.build());
				});
	}

	@PostMapping(value = "/items", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> changeItemFromCatalog(@Valid @ModelAttribute ChangeItemCountRequestDto request, WebSession session) {
		return cartService.applyCartAction(request.action(), request.id(), session)
				.thenReturn("redirect:/items");
	}

	@GetMapping(value = "/items/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> itemPage(@PathVariable("id") @Positive Long id, WebSession session) {
		return cartService.getCart(session)
				.defaultIfEmpty(new CartStateResponseDto(Collections.emptyList(), 0L))
				.zipWith(itemService.getItem(id))
				.flatMap(tuple -> {
					CartStateResponseDto cart = tuple.getT1();
					var item = tuple.getT2();
					return Flux.fromIterable(cart.items())
							.filter(ci -> ci.id().equals(id))
							.next()
							.map(CartItemResponseDto::count)
							.defaultIfEmpty(0)
							.map(countInCart -> {
								var enrichedItem = new ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto(
										item.id(),
										item.title(),
										item.description(),
										item.imgPath(),
										item.price(),
										countInCart
								);
								return Rendering.view("item")
										.modelAttribute("item", enrichedItem)
										.modelAttribute("total", cart.total())
										.build();
							});
				});
	}

	@PostMapping(value = "/items/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> changeItemFromDetails(@PathVariable("id") @Positive Long id,
	                                          @ModelAttribute @Valid ChangeItemCountRequestDto request,
	                                          WebSession session) {
		return cartService.applyCartAction(request.action(), id, session)
				.thenReturn("redirect:/items/" + id);
	}

	@GetMapping(value = "/cart/items", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> cartPage(WebSession session) {
		return cartService.getCart(session)
				.defaultIfEmpty(new CartStateResponseDto(Collections.emptyList(), 0L))
				.map(cart -> Rendering.view("cart")
						.modelAttribute("items", cart.items())
						.modelAttribute("total", cart.total())
						.build());
	}

	@PostMapping(value = "/cart/items", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> updateCartFromCart(@Valid @ModelAttribute CartUpdateRequestDto request,
	                                       WebSession session) {
		return cartService.applyCartAction(request.action(), request.id(), session)
				.thenReturn("redirect:/cart/items");
	}

	@PostMapping(value = "/buy", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> buy(WebSession session) {
		return orderService.buy(session)
				.map(order -> "redirect:/orders/" + order.id() + "?newOrder=true");
	}

	@GetMapping(value = "/orders", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> ordersPage() {
		return orderService.getOrders()
				.collectList()
				.map(orders -> Rendering.view("orders")
						.modelAttribute("orders", orders)
						.build());
	}

	@GetMapping(value = "/orders/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> orderPage(@PathVariable("id") @Positive long id,
	                                 @RequestParam(value = "newOrder", required = false) Boolean newOrder) {
		return orderService.getOrder(id)
				.map(order -> Rendering.view("order")
						.modelAttribute("order", order)
						.modelAttribute("newOrder", Optional.ofNullable(newOrder).orElse(false))
						.build());
	}
}
