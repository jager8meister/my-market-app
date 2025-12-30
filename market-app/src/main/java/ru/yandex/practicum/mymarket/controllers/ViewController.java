package ru.yandex.practicum.mymarket.controllers;

import java.util.Optional;

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
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.model.CommonViewAttributes;
import ru.yandex.practicum.mymarket.dto.request.CartActionWithNavigationDto;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.exception.UserNotFoundException;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentServiceHealthCheck;
import ru.yandex.practicum.mymarket.service.UserService;

@Slf4j
@Controller
@RequiredArgsConstructor
@Validated
public class ViewController {

	private final ItemService itemService;
	private final CartService cartService;
	private final OrderService orderService;
	private final PaymentServiceHealthCheck paymentServiceHealthCheck;
	private final UserService userService;

	private Mono<String> getFormattedBalance() {
		return userService.getCurrentUserId()
				.flatMap(userId -> userService.getUserBalance(userId)
						.map(balance -> {
							if (balance == -1L) {
								return "Баланс недоступен";
							}
							return String.format("%,d ₽", balance);
						})
						.onErrorReturn("Баланс недоступен"))
				.onErrorResume(UserNotFoundException.class, ex -> {
					log.warn("User not found when getting balance, user needs to re-login");
					return Mono.just("Баланс недоступен");
				});
	}

	private Mono<String> getFormattedBalanceForAnonymous() {
		return userService.getCurrentUserId()
				.flatMap(userId -> userService.getUserBalance(userId)
						.map(balance -> {
							if (balance == -1L) {
								return "—";
							}
							return String.format("%,d ₽", balance);
						})
						.onErrorReturn("—"))
				.onErrorReturn("—");
	}

	private Mono<CommonViewAttributes> getCommonAttributesForAuthenticated() {
		return Mono.zip(
				getFormattedBalance(),
				userService.getCurrentUser().map(user -> user.getUsername()),
				Mono.just(true)
		).map(tuple -> new CommonViewAttributes(tuple.getT1(), tuple.getT2(), tuple.getT3()));
	}

	private Mono<CommonViewAttributes> getCommonAttributesForAnonymous() {
		return Mono.zip(
				getFormattedBalanceForAnonymous(),
				userService.getCurrentUser().map(user -> user.getUsername()).onErrorReturn(""),
				userService.getCurrentUserId().map(id -> true).onErrorReturn(false)
		).map(tuple -> new CommonViewAttributes(tuple.getT1(), tuple.getT2(), tuple.getT3()));
	}

	private Rendering.Builder addCommonAttributes(Rendering.Builder builder, CommonViewAttributes attributes) {
		return builder
				.modelAttribute("balance", attributes.balance())
				.modelAttribute("username", attributes.username())
				.modelAttribute("isAuthenticated", attributes.isAuthenticated());
	}

	@GetMapping(value = {"/", "/items"}, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> itemsPage(
			@ModelAttribute @Valid ItemsFilterRequestDto filter,
			@RequestParam(defaultValue = "1") @Positive int pageNumber,
			@RequestParam(defaultValue = "5") @Positive int pageSize,
			WebSession session) {
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

		return Mono.zip(
				cartService.getCart(session).onErrorReturn(new CartStateResponseDto(java.util.List.of(), 0L)),
				getCommonAttributesForAnonymous()
		)
		.flatMap(tuple -> {
			CartStateResponseDto cart = tuple.getT1();
			CommonViewAttributes commonAttrs = tuple.getT2();
			return itemService.getItemsWithCartCounts(filter, pageable, cart)
					.map(page -> addCommonAttributes(
							Rendering.view("items")
									.modelAttribute("items", page.getContent())
									.modelAttribute("page", page)
									.modelAttribute("search", filter.search())
									.modelAttribute("sort", filter.sort()),
							commonAttrs
					).build());
		});
	}

	@PostMapping(value = "/items", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> changeItemFromCatalog(
			@Valid @ModelAttribute CartActionWithNavigationDto params,
			WebSession session) {
		return cartService.applyCartActionAndBuildRedirect(params, session);
	}

	@GetMapping(value = "/items/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> itemPage(@PathVariable("id") @Positive Long id, WebSession session) {
		return Mono.zip(
				cartService.getCart(session).onErrorReturn(new CartStateResponseDto(java.util.List.of(), 0L)),
				cartService.getItemCountInCart(id, session).onErrorReturn(0),
				getCommonAttributesForAnonymous()
		)
		.flatMap(tuple -> {
			CartStateResponseDto cart = tuple.getT1();
			int countInCart = tuple.getT2();
			CommonViewAttributes commonAttrs = tuple.getT3();
			return itemService.getItemWithCartCount(id, countInCart)
					.map(item -> addCommonAttributes(
							Rendering.view("item")
									.modelAttribute("item", item)
									.modelAttribute("total", cart.total()),
							commonAttrs
					).build());
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
	public Mono<Rendering> cartPage(@RequestParam(required = false) String error, WebSession session) {
		boolean paymentServiceAvailable = paymentServiceHealthCheck.isPaymentServiceAvailable();
		return Mono.zip(
				cartService.getCart(session),
				getCommonAttributesForAuthenticated(),
				userService.getCurrentUserId()
		)
		.flatMap(tuple -> {
			CartStateResponseDto cart = tuple.getT1();
			CommonViewAttributes commonAttrs = tuple.getT2();
			Long userId = tuple.getT3();

			return userService.getUserBalance(userId)
					.map(balanceAmount -> {
						boolean hasEnoughBalance = balanceAmount >= cart.total();

						Rendering.Builder builder = addCommonAttributes(
								Rendering.view("cart")
										.modelAttribute("items", cart.items())
										.modelAttribute("total", cart.total())
										.modelAttribute("paymentServiceAvailable", paymentServiceAvailable)
										.modelAttribute("hasEnoughBalance", hasEnoughBalance),
								commonAttrs
						);

						if (error != null && !error.isBlank()) {
							builder.modelAttribute("error", error);
						}

						return builder.build();
					});
		})
		.onErrorResume(UserNotFoundException.class, ex -> {
			log.warn("User not found in cartPage, redirecting to login");
			return Mono.just(Rendering.redirectTo("/login?error=session_expired").build());
		});
	}

	@PostMapping(value = "/cart/items", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> updateCartFromCart(@Valid @ModelAttribute CartUpdateRequestDto request,
	                                       WebSession session) {
		return cartService.applyCartAction(request.action(), request.id(), session)
				.thenReturn("redirect:/cart/items");
	}

	@PostMapping(value = "/buy", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> buy(WebSession session) {
		if (!paymentServiceHealthCheck.isPaymentServiceAvailable()) {
			return Mono.just("redirect:/cart/items?error=Payment+service+temporarily+unavailable");
		}

		return orderService.buy(session)
				.map(order -> "redirect:/orders/" + order.id() + "?newOrder=true")
				.onErrorResume(error -> {
					String errorMessage = error.getMessage();
					if (errorMessage == null || errorMessage.isBlank()) {
						errorMessage = "An+error+occurred+while+processing+order";
					} else {
						errorMessage = errorMessage.replace(" ", "+");
					}
					return Mono.just("redirect:/cart/items?error=" + errorMessage);
				});
	}

	@GetMapping(value = "/orders", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> ordersPage() {
		return Mono.zip(
				orderService.getOrders().collectList(),
				getCommonAttributesForAuthenticated()
		)
		.map(tuple -> addCommonAttributes(
				Rendering.view("orders")
						.modelAttribute("orders", tuple.getT1()),
				tuple.getT2()
		).build())
		.onErrorResume(UserNotFoundException.class, ex -> {
			log.warn("User not found in ordersPage, redirecting to login");
			return Mono.just(Rendering.redirectTo("/login?error=session_expired").build());
		});
	}

	@GetMapping(value = "/orders/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Rendering> orderPage(@PathVariable("id") @Positive long id,
	                                 @RequestParam(value = "newOrder", required = false) Boolean newOrder) {
		return Mono.zip(
				orderService.getOrder(id),
				getCommonAttributesForAuthenticated()
		)
		.map(tuple -> addCommonAttributes(
				Rendering.view("order")
						.modelAttribute("order", tuple.getT1())
						.modelAttribute("newOrder", Optional.ofNullable(newOrder).orElse(false)),
				tuple.getT2()
		).build())
		.onErrorResume(UserNotFoundException.class, ex -> {
			log.warn("User not found in orderPage, redirecting to login");
			return Mono.just(Rendering.redirectTo("/login?error=session_expired").build());
		});
	}
}
