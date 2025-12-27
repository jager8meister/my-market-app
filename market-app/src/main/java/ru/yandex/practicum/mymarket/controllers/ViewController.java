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

	private <T> Mono<T> handleUserNotFound(Throwable ex, T redirectValue) {
		if (ex instanceof UserNotFoundException) {
			log.warn("User not found, redirecting to login: {}", ex.getMessage());
			return Mono.just(redirectValue);
		}
		return Mono.error(ex);
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
				getFormattedBalanceForAnonymous(),
				userService.getCurrentUser().map(user -> user.getUsername()).onErrorReturn(""),
				userService.getCurrentUserId().map(id -> true).onErrorReturn(false)
		)
		.flatMap(tuple -> {
			CartStateResponseDto cart = tuple.getT1();
			String balance = tuple.getT2();
			String username = tuple.getT3();
			Boolean isAuthenticated = tuple.getT4();
			return itemService.getItemsWithCartCounts(filter, pageable, cart)
					.map(page -> Rendering.view("items")
							.modelAttribute("items", page.getContent())
							.modelAttribute("page", page)
							.modelAttribute("search", filter.search())
							.modelAttribute("sort", filter.sort())
							.modelAttribute("balance", balance)
							.modelAttribute("username", username)
							.modelAttribute("isAuthenticated", isAuthenticated)
							.build());
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
				getFormattedBalanceForAnonymous(),
				userService.getCurrentUser().map(user -> user.getUsername()).onErrorReturn(""),
				userService.getCurrentUserId().map(userId -> true).onErrorReturn(false)
		)
		.flatMap(tuple -> {
			CartStateResponseDto cart = tuple.getT1();
			int countInCart = tuple.getT2();
			String balance = tuple.getT3();
			String username = tuple.getT4();
			Boolean isAuthenticated = tuple.getT5();
			return itemService.getItemWithCartCount(id, countInCart)
					.map(item -> Rendering.view("item")
							.modelAttribute("item", item)
							.modelAttribute("total", cart.total())
							.modelAttribute("balance", balance)
							.modelAttribute("username", username)
							.modelAttribute("isAuthenticated", isAuthenticated)
							.build());
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
				getFormattedBalance(),
				userService.getCurrentUserId(),
				userService.getCurrentUser().map(user -> user.getUsername())
		)
		.flatMap(tuple -> {
			CartStateResponseDto cart = tuple.getT1();
			String balanceFormatted = tuple.getT2();
			Long userId = tuple.getT3();
			String username = tuple.getT4();

			return userService.getUserBalance(userId)
					.map(balanceAmount -> {
						boolean hasEnoughBalance = balanceAmount >= cart.total();

						Rendering.Builder builder = Rendering.view("cart")
								.modelAttribute("items", cart.items())
								.modelAttribute("total", cart.total())
								.modelAttribute("paymentServiceAvailable", paymentServiceAvailable)
								.modelAttribute("balance", balanceFormatted)
								.modelAttribute("hasEnoughBalance", hasEnoughBalance)
								.modelAttribute("username", username)
								.modelAttribute("isAuthenticated", true);

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
				getFormattedBalance(),
				userService.getCurrentUser().map(user -> user.getUsername()),
				userService.getCurrentUserId().map(id -> true)
		)
		.map(tuple -> Rendering.view("orders")
				.modelAttribute("orders", tuple.getT1())
				.modelAttribute("balance", tuple.getT2())
				.modelAttribute("username", tuple.getT3())
				.modelAttribute("isAuthenticated", tuple.getT4())
				.build())
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
				getFormattedBalance(),
				userService.getCurrentUser().map(user -> user.getUsername()),
				userService.getCurrentUserId().map(id2 -> true)
		)
		.map(tuple -> Rendering.view("order")
				.modelAttribute("order", tuple.getT1())
				.modelAttribute("newOrder", Optional.ofNullable(newOrder).orElse(false))
				.modelAttribute("balance", tuple.getT2())
				.modelAttribute("username", tuple.getT3())
				.modelAttribute("isAuthenticated", tuple.getT4())
				.build())
		.onErrorResume(UserNotFoundException.class, ex -> {
			log.warn("User not found in orderPage, redirecting to login");
			return Mono.just(Rendering.redirectTo("/login?error=session_expired").build());
		});
	}
}
