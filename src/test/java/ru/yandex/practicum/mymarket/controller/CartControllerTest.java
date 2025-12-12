package ru.yandex.practicum.mymarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.controllers.CartController;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

@WebMvcTest(CartController.class)
class CartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CartService cartService;

	@MockBean
	private OrderService orderService;

	@Test
	void shouldShowCartPage() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(0);
			model.addAttribute("items", new ArrayList<>());
			model.addAttribute("total", 0L);
			return "cart";
		}).when(cartService).showCart(any(Model.class));

		mockMvc.perform(get("/cart/items"))
				.andExpect(status().isOk())
				.andExpect(view().name("cart"));

		verify(cartService, times(1)).showCart(any(Model.class));
	}

	@Test
	void shouldUpdateCartWithPlusAction() throws Exception {
		when(cartService.updateCart(any(CartUpdateRequestDto.class)))
				.thenReturn("redirect:/cart/items");

		mockMvc.perform(post("/cart/items")
						.param("id", "1")
						.param("action", "PLUS"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart/items"));

		verify(cartService, times(1)).updateCart(any(CartUpdateRequestDto.class));
	}

	@Test
	void shouldUpdateCartWithMinusAction() throws Exception {
		when(cartService.updateCart(any(CartUpdateRequestDto.class)))
				.thenReturn("redirect:/cart/items");

		mockMvc.perform(post("/cart/items")
						.param("id", "2")
						.param("action", "MINUS"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart/items"));

		verify(cartService, times(1)).updateCart(any(CartUpdateRequestDto.class));
	}

	@Test
	void shouldUpdateCartWithDeleteAction() throws Exception {
		when(cartService.updateCart(any(CartUpdateRequestDto.class)))
				.thenReturn("redirect:/cart/items");

		mockMvc.perform(post("/cart/items")
						.param("id", "3")
						.param("action", "DELETE"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart/items"));

		verify(cartService, times(1)).updateCart(any(CartUpdateRequestDto.class));
	}

	@Test
	void shouldHandleBuyRequest() throws Exception {
		when(orderService.buy())
				.thenReturn("redirect:/orders/1?newOrder=true");

		mockMvc.perform(post("/buy"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/orders/1?newOrder=true"));

		verify(orderService, times(1)).buy();
	}

	@Test
	void shouldCallBuyOnlyOnce() throws Exception {
		when(orderService.buy())
				.thenReturn("redirect:/orders/2?newOrder=true");

		mockMvc.perform(post("/buy"))
				.andExpect(status().is3xxRedirection());

		verify(orderService, times(1)).buy();
	}
}
