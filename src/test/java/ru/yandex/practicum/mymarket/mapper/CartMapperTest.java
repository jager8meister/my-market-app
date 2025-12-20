package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

class CartMapperTest {

	private final CartMapper mapper = new CartMapperImpl();

	@Test
	void mapsCartEntryToDto() {
		ItemEntity item = new ItemEntity(1L, "Title", "Desc", 100L, "img");
		CartEntry entry = new CartEntry(item, 3);

		CartItemResponseDto dto = mapper.toCartItemResponse(entry);

		assertEquals(1L, dto.id());
		assertEquals("Title", dto.title());
		assertEquals("Desc", dto.description());
		assertEquals(100L, dto.price());
		assertEquals("img", dto.imgPath());
		assertEquals(3, dto.count());
	}
}
