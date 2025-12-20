package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;

class OrderMapperTest {

	private final OrderMapper mapper = new OrderMapperImpl();

	@Test
	void toOrderItemResponseCopiesFields() {
		OrderItemEntity entity = new OrderItemEntity(1L, 100L, "Name", 123L, 2);
		OrderItemResponseDto dto = mapper.toOrderItemResponse(entity);
		assertEquals("Name", dto.title());
		assertEquals(123L, dto.price());
		assertEquals(2, dto.count());
	}
}
