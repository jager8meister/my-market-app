package ru.yandex.practicum.mymarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.controllers.ItemDetailsController;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebMvcTest(ItemDetailsController.class)
class ItemDetailsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	@MockBean
	private CartService cartService;

	@Test
	void shouldShowItemDetailsPage() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			ItemDetailsResponseDto itemDto = new ItemDetailsResponseDto(1L, "Test Item", "Description", "/img/test.jpg", 10000L, 0);
			model.addAttribute("item", itemDto);
			return "item";
		}).when(itemService).showItemDetails(eq(1L), any(Model.class));

		mockMvc.perform(get("/items/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("item"));

		verify(itemService, times(1)).showItemDetails(eq(1L), any(Model.class));
	}

	@Test
	void shouldShowItemDetailsForDifferentIds() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			ItemDetailsResponseDto itemDto = new ItemDetailsResponseDto(5L, "Another Item", "Description", "/img/test.jpg", 20000L, 2);
			model.addAttribute("item", itemDto);
			return "item";
		}).when(itemService).showItemDetails(eq(5L), any(Model.class));

		mockMvc.perform(get("/items/5"))
				.andExpect(status().isOk())
				.andExpect(view().name("item"));

		verify(itemService, times(1)).showItemDetails(eq(5L), any(Model.class));
	}

	@Test
	void shouldChangeItemCountWithPlusAction() throws Exception {
		when(cartService.changeItemCountOnDetails(eq(3L), eq(CartAction.PLUS)))
				.thenReturn("redirect:/items/3");

		mockMvc.perform(post("/items/3")
						.param("action", "PLUS"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/items/3"));

		verify(cartService, times(1)).changeItemCountOnDetails(eq(3L), eq(CartAction.PLUS));
	}

	@Test
	void shouldChangeItemCountWithMinusAction() throws Exception {
		when(cartService.changeItemCountOnDetails(eq(2L), eq(CartAction.MINUS)))
				.thenReturn("redirect:/items/2");

		mockMvc.perform(post("/items/2")
						.param("action", "MINUS"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/items/2"));

		verify(cartService, times(1)).changeItemCountOnDetails(eq(2L), eq(CartAction.MINUS));
	}

	@Test
	void shouldHandleDifferentItemIdsInPostRequest() throws Exception {
		when(cartService.changeItemCountOnDetails(eq(10L), eq(CartAction.PLUS)))
				.thenReturn("redirect:/items/10");

		mockMvc.perform(post("/items/10")
						.param("action", "PLUS"))
				.andExpect(status().is3xxRedirection());

		verify(cartService, times(1)).changeItemCountOnDetails(eq(10L), eq(CartAction.PLUS));
	}

	@Test
	void shouldHandleItemNotFoundException() throws Exception {
		doThrow(new ItemNotFoundException("Item not found with id: 999"))
				.when(itemService).showItemDetails(eq(999L), any(Model.class));

		mockMvc.perform(get("/items/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Item not found with id: 999"));
	}

	@Test
	void shouldHandleEmptyCartException() throws Exception {
		when(cartService.changeItemCountOnDetails(eq(1L), eq(CartAction.PLUS)))
				.thenThrow(new EmptyCartException("Cannot create order from empty cart"));

		mockMvc.perform(post("/items/1")
						.param("action", "PLUS"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Cannot create order from empty cart"));
	}

	@Test
	void shouldHandleIllegalArgumentException() throws Exception {
		doThrow(new IllegalArgumentException("Invalid item ID"))
				.when(itemService).showItemDetails(eq(-1L), any(Model.class));

		mockMvc.perform(get("/items/-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid item ID"));
	}

	@Test
	void shouldHandleGenericException() throws Exception {
		doThrow(new RuntimeException("Unexpected error"))
				.when(itemService).showItemDetails(eq(1L), any(Model.class));

		mockMvc.perform(get("/items/1"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("An unexpected error occurred: Unexpected error"));
	}
}
