package ru.yandex.practicum.mymarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.controllers.ItemsController;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.PagingDto;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.enums.SortType;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebMvcTest(ItemsController.class)
class ItemsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	@MockBean
	private CartService cartService;

	@Test
	void shouldShowItemsPageWithDefaultParameters() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("search", "");
			model.addAttribute("sort", SortType.NO);
			model.addAttribute("paging", new PagingDto(5, 1, false, false));
			model.addAttribute("items", new ArrayList<List<ItemResponseDto>>());
			return "items";
		}).when(itemService).showItems(any(ItemsFilterRequestDto.class), any(Model.class));

		mockMvc.perform(get("/items"))
				.andExpect(status().isOk())
				.andExpect(view().name("items"));

		verify(itemService, times(1)).showItems(any(ItemsFilterRequestDto.class), any(Model.class));
	}

	@Test
	void shouldShowItemsPageWithSearchParameter() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("search", "смартфон");
			model.addAttribute("sort", SortType.NO);
			model.addAttribute("paging", new PagingDto(5, 1, false, false));
			model.addAttribute("items", new ArrayList<List<ItemResponseDto>>());
			return "items";
		}).when(itemService).showItems(any(ItemsFilterRequestDto.class), any(Model.class));

		mockMvc.perform(get("/items")
						.param("search", "смартфон"))
				.andExpect(status().isOk())
				.andExpect(view().name("items"));
	}

	@Test
	void shouldShowItemsPageWithSortParameter() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("search", "");
			model.addAttribute("sort", SortType.PRICE);
			model.addAttribute("paging", new PagingDto(5, 1, false, false));
			model.addAttribute("items", new ArrayList<List<ItemResponseDto>>());
			return "items";
		}).when(itemService).showItems(any(ItemsFilterRequestDto.class), any(Model.class));

		mockMvc.perform(get("/items")
						.param("sort", "PRICE"))
				.andExpect(status().isOk())
				.andExpect(view().name("items"));
	}

	@Test
	void shouldShowItemsPageWithPaginationParameters() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("search", "");
			model.addAttribute("sort", SortType.NO);
			model.addAttribute("paging", new PagingDto(10, 2, true, false));
			model.addAttribute("items", new ArrayList<List<ItemResponseDto>>());
			return "items";
		}).when(itemService).showItems(any(ItemsFilterRequestDto.class), any(Model.class));

		mockMvc.perform(get("/items")
						.param("pageNumber", "2")
						.param("pageSize", "10"))
				.andExpect(status().isOk())
				.andExpect(view().name("items"));
	}

	@Test
	void shouldShowItemsPageWithAllParameters() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("search", "смартфон");
			model.addAttribute("sort", SortType.ALPHA);
			model.addAttribute("paging", new PagingDto(20, 3, true, false));
			model.addAttribute("items", new ArrayList<List<ItemResponseDto>>());
			return "items";
		}).when(itemService).showItems(any(ItemsFilterRequestDto.class), any(Model.class));

		mockMvc.perform(get("/items")
						.param("search", "смартфон")
						.param("sort", "ALPHA")
						.param("pageNumber", "3")
						.param("pageSize", "20"))
				.andExpect(status().isOk())
				.andExpect(view().name("items"));
	}

	@Test
	void shouldShowRootPageWithDefaultParameters() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("search", "");
			model.addAttribute("sort", SortType.NO);
			model.addAttribute("paging", new PagingDto(5, 1, false, false));
			model.addAttribute("items", new ArrayList<List<ItemResponseDto>>());
			return "items";
		}).when(itemService).showItems(any(ItemsFilterRequestDto.class), any(Model.class));

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("items"));
	}

	@Test
	void shouldChangeItemCountAndRedirect() throws Exception {
		when(cartService.changeItemCount(any(ChangeItemCountRequestDto.class)))
				.thenReturn("redirect:/items?search=&sort=NO&pageNumber=1&pageSize=5");

		mockMvc.perform(post("/items")
						.param("id", "1")
						.param("action", "PLUS")
						.param("search", "")
						.param("sort", "NO")
						.param("pageNumber", "1")
						.param("pageSize", "5"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=5"));

		verify(cartService, times(1)).changeItemCount(any(ChangeItemCountRequestDto.class));
	}

	@Test
	void shouldChangeItemCountWithPlusAction() throws Exception {
		when(cartService.changeItemCount(any(ChangeItemCountRequestDto.class)))
				.thenReturn("redirect:/items?search=test&sort=PRICE&pageNumber=2&pageSize=10");

		mockMvc.perform(post("/items")
						.param("id", "5")
						.param("action", "PLUS")
						.param("search", "test")
						.param("sort", "PRICE")
						.param("pageNumber", "2")
						.param("pageSize", "10"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void shouldChangeItemCountWithMinusAction() throws Exception {
		when(cartService.changeItemCount(any(ChangeItemCountRequestDto.class)))
				.thenReturn("redirect:/items?search=&sort=NO&pageNumber=1&pageSize=5");

		mockMvc.perform(post("/items")
						.param("id", "3")
						.param("action", "MINUS")
						.param("search", "")
						.param("sort", "NO")
						.param("pageNumber", "1")
						.param("pageSize", "5"))
				.andExpect(status().is3xxRedirection());
	}
}
