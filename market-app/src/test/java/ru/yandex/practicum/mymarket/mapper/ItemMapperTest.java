package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;

class ItemMapperTest {

	private final ItemMapper mapper = new ItemMapperImpl();

	@Test
	void toItemResponseCopiesFields() {
		ItemEntity entity = new ItemEntity(1L, "T", "D", 50L, "img");
		ItemResponseDto dto = mapper.toItemResponse(entity, 2);
		assertEquals(1L, dto.id());
		assertEquals("T", dto.title());
		assertEquals("D", dto.description());
		assertEquals(50L, dto.price());
		assertEquals("img", dto.imgPath());
		assertEquals(2, dto.count());
	}

	@Test
	void toItemDetailsCopiesFields() {
		ItemEntity entity = new ItemEntity(2L, "X", "Y", 70L, "path");
		ItemDetailsResponseDto dto = mapper.toItemDetailsResponse(entity, 1);
		assertEquals(2L, dto.id());
		assertEquals("X", dto.title());
		assertEquals("Y", dto.description());
		assertEquals(70L, dto.price());
		assertEquals("path", dto.imgPath());
		assertEquals(1, dto.count());
	}
}
