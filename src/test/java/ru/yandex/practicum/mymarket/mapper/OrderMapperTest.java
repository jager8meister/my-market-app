package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.service.model.OrderItemModel;
import ru.yandex.practicum.mymarket.service.model.OrderModel;

class OrderMapperTest {

	private final OrderMapper mapper = new OrderMapperImpl();

	@Test
	void toOrderItemResponseCopiesFields() {
		OrderItemModel model = new OrderItemModel("Name", 123L, 2);
		OrderItemResponseDto dto = mapper.toOrderItemResponse(model);
		assertEquals("Name", dto.title());
		assertEquals(123L, dto.price());
		assertEquals(2, dto.count());
	}

	@Test
	void toOrderResponseCopiesFields() {
		OrderModel model = new OrderModel(5L, List.of(), 777L, LocalDateTime.now());
		OrderResponseDto dto = mapper.toOrderResponse(model);
		assertEquals(5L, dto.id());
		assertEquals(777L, dto.totalSum());
		assertEquals(model.getCreatedAt(), dto.createdAt());
	}
}
