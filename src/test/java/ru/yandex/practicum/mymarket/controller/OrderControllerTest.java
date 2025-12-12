package ru.yandex.practicum.mymarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.controllers.OrderController;
import ru.yandex.practicum.mymarket.dto.request.OrderDetailsRequestDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OrderService orderService;

	@Test
	void shouldShowOrdersPageWithDefaultPagination() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("orders", new ArrayList<>());
			model.addAttribute("pageNumber", 1);
			model.addAttribute("pageSize", 10);
			model.addAttribute("totalPages", 1);
			model.addAttribute("hasNext", false);
			model.addAttribute("hasPrevious", false);
			return "orders";
		}).when(orderService).showOrders(any(Pageable.class), any(Model.class));

		mockMvc.perform(get("/orders"))
				.andExpect(status().isOk())
				.andExpect(view().name("orders"));

		verify(orderService, times(1)).showOrders(any(Pageable.class), any(Model.class));
	}

	@Test
	void shouldShowOrdersPageWithCustomPagination() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			model.addAttribute("orders", new ArrayList<>());
			model.addAttribute("pageNumber", 3);
			model.addAttribute("pageSize", 20);
			model.addAttribute("totalPages", 5);
			model.addAttribute("hasNext", true);
			model.addAttribute("hasPrevious", true);
			return "orders";
		}).when(orderService).showOrders(any(Pageable.class), any(Model.class));

		mockMvc.perform(get("/orders")
						.param("page", "2")
						.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(view().name("orders"));

		verify(orderService, times(1)).showOrders(any(Pageable.class), any(Model.class));
	}

	@Test
	void shouldShowOrderDetailsWithoutNewOrderFlag() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			OrderResponseDto orderDto = new OrderResponseDto(1L, new ArrayList<>(), 10000L);
			model.addAttribute("order", orderDto);
			model.addAttribute("items", new ArrayList<>());
			model.addAttribute("newOrder", false);
			return "order";
		}).when(orderService).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));

		mockMvc.perform(get("/orders/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("order"));

		verify(orderService, times(1)).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));
	}

	@Test
	void shouldShowOrderDetailsWithNewOrderFlagTrue() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			OrderResponseDto orderDto = new OrderResponseDto(5L, new ArrayList<>(), 20000L);
			model.addAttribute("order", orderDto);
			model.addAttribute("items", new ArrayList<>());
			model.addAttribute("newOrder", true);
			return "order";
		}).when(orderService).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));

		mockMvc.perform(get("/orders/5")
						.param("newOrder", "true"))
				.andExpect(status().isOk())
				.andExpect(view().name("order"));

		verify(orderService, times(1)).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));
	}

	@Test
	void shouldShowOrderDetailsWithNewOrderFlagFalse() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			OrderResponseDto orderDto = new OrderResponseDto(3L, new ArrayList<>(), 15000L);
			model.addAttribute("order", orderDto);
			model.addAttribute("items", new ArrayList<>());
			model.addAttribute("newOrder", false);
			return "order";
		}).when(orderService).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));

		mockMvc.perform(get("/orders/3")
						.param("newOrder", "false"))
				.andExpect(status().isOk())
				.andExpect(view().name("order"));

		verify(orderService, times(1)).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));
	}

	@Test
	void shouldHandleDifferentOrderIds() throws Exception {
		doAnswer(invocation -> {
			Model model = invocation.getArgument(1);
			OrderResponseDto orderDto = new OrderResponseDto(100L, new ArrayList<>(), 50000L);
			model.addAttribute("order", orderDto);
			model.addAttribute("items", new ArrayList<>());
			model.addAttribute("newOrder", false);
			return "order";
		}).when(orderService).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));

		mockMvc.perform(get("/orders/100"))
				.andExpect(status().isOk())
				.andExpect(view().name("order"));

		verify(orderService, times(1)).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));
	}

	@Test
	void shouldHandleOrderNotFoundException() throws Exception {
		doThrow(new OrderNotFoundException("Order not found with id: 999"))
				.when(orderService).showOrder(any(OrderDetailsRequestDto.class), any(Model.class));

		mockMvc.perform(get("/orders/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Order not found with id: 999"));
	}
}
