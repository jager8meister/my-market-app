package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;

@SpringBootTest
@ActiveProfiles("test")
class ItemMapperTest {

	@Autowired
	private ItemMapper itemMapper;

	@Test
	void shouldMapToItemResponse() {
		// given
		ItemEntity entity = new ItemEntity();
		entity.setId(1L);
		entity.setTitle("Test Item");
		entity.setDescription("Test Description");
		entity.setPrice(10000L);
		entity.setImgPath("/images/test.jpg");
		int count = 5;

		// when
		ItemResponseDto dto = itemMapper.toItemResponse(entity, count);

		// then
		assertNotNull(dto);
		assertEquals(1L, dto.getId());
		assertEquals("Test Item", dto.getTitle());
		assertEquals("Test Description", dto.getDescription());
		assertEquals(10000L, dto.getPrice());
		assertEquals("/images/test.jpg", dto.getImgPath());
		assertEquals(5, dto.getCount());
	}

	@Test
	void shouldMapToItemResponseWithZeroCount() {
		// given
		ItemEntity entity = new ItemEntity();
		entity.setId(2L);
		entity.setTitle("Another Item");
		entity.setDescription("Another Description");
		entity.setPrice(20000L);
		entity.setImgPath("/images/another.png");
		int count = 0;

		// when
		ItemResponseDto dto = itemMapper.toItemResponse(entity, count);

		// then
		assertNotNull(dto);
		assertEquals(2L, dto.getId());
		assertEquals("Another Item", dto.getTitle());
		assertEquals("Another Description", dto.getDescription());
		assertEquals(20000L, dto.getPrice());
		assertEquals("/images/another.png", dto.getImgPath());
		assertEquals(0, dto.getCount());
	}

	@Test
	void shouldMapToItemResponseWithNullFields() {
		// given
		ItemEntity entity = new ItemEntity();
		entity.setId(3L);
		entity.setTitle("Item");
		entity.setPrice(5000L);
		// description and imgPath are null
		int count = 1;

		// when
		ItemResponseDto dto = itemMapper.toItemResponse(entity, count);

		// then
		assertNotNull(dto);
		assertEquals(3L, dto.getId());
		assertEquals("Item", dto.getTitle());
		assertNull(dto.getDescription());
		assertEquals(5000L, dto.getPrice());
		assertNull(dto.getImgPath());
		assertEquals(1, dto.getCount());
	}

	@Test
	void shouldMapToItemDetailsResponse() {
		// given
		ItemEntity entity = new ItemEntity();
		entity.setId(10L);
		entity.setTitle("Detailed Item");
		entity.setDescription("Detailed Description with more info");
		entity.setPrice(30000L);
		entity.setImgPath("/images/detailed.svg");
		int count = 3;

		// when
		ItemDetailsResponseDto dto = itemMapper.toItemDetailsResponse(entity, count);

		// then
		assertNotNull(dto);
		assertEquals(10L, dto.getId());
		assertEquals("Detailed Item", dto.getTitle());
		assertEquals("Detailed Description with more info", dto.getDescription());
		assertEquals(30000L, dto.getPrice());
		assertEquals("/images/detailed.svg", dto.getImgPath());
		assertEquals(3, dto.getCount());
	}

	@Test
	void shouldMapToItemDetailsResponseWithZeroCount() {
		// given
		ItemEntity entity = new ItemEntity();
		entity.setId(11L);
		entity.setTitle("Item Without Cart");
		entity.setDescription("Description");
		entity.setPrice(15000L);
		entity.setImgPath("/images/no-cart.jpg");
		int count = 0;

		// when
		ItemDetailsResponseDto dto = itemMapper.toItemDetailsResponse(entity, count);

		// then
		assertNotNull(dto);
		assertEquals(11L, dto.getId());
		assertEquals("Item Without Cart", dto.getTitle());
		assertEquals("Description", dto.getDescription());
		assertEquals(15000L, dto.getPrice());
		assertEquals("/images/no-cart.jpg", dto.getImgPath());
		assertEquals(0, dto.getCount());
	}

	@Test
	void shouldMapToItemDetailsResponseWithNullFields() {
		// given
		ItemEntity entity = new ItemEntity();
		entity.setId(12L);
		entity.setTitle("Minimal Item");
		entity.setPrice(1000L);
		// description and imgPath are null
		int count = 2;

		// when
		ItemDetailsResponseDto dto = itemMapper.toItemDetailsResponse(entity, count);

		// then
		assertNotNull(dto);
		assertEquals(12L, dto.getId());
		assertEquals("Minimal Item", dto.getTitle());
		assertNull(dto.getDescription());
		assertEquals(1000L, dto.getPrice());
		assertNull(dto.getImgPath());
		assertEquals(2, dto.getCount());
	}

	@Test
	void shouldMapToEntity() {
		// given
		ItemResponseDto dto = new ItemResponseDto();
		dto.setId(20L);
		dto.setTitle("DTO Item");
		dto.setDescription("DTO Description");
		dto.setPrice(40000L);
		dto.setImgPath("/images/dto.png");
		dto.setCount(7);

		// when
		ItemEntity entity = itemMapper.toEntity(dto);

		// then
		assertNotNull(entity);
		assertEquals(20L, entity.getId());
		assertEquals("DTO Item", entity.getTitle());
		assertEquals("DTO Description", entity.getDescription());
		assertEquals(40000L, entity.getPrice());
		// imgPath is ignored in mapping
		assertNull(entity.getImgPath());
	}

	@Test
	void shouldMapToEntityWithNullImgPath() {
		// given
		ItemResponseDto dto = new ItemResponseDto();
		dto.setId(21L);
		dto.setTitle("No Image Item");
		dto.setDescription("No image description");
		dto.setPrice(5000L);
		dto.setImgPath(null);
		dto.setCount(1);

		// when
		ItemEntity entity = itemMapper.toEntity(dto);

		// then
		assertNotNull(entity);
		assertEquals(21L, entity.getId());
		assertEquals("No Image Item", entity.getTitle());
		assertEquals("No image description", entity.getDescription());
		assertEquals(5000L, entity.getPrice());
		// imgPath is ignored in mapping, should be null
		assertNull(entity.getImgPath());
	}

	@Test
	void shouldMapToEntityIgnoringCount() {
		// given
		ItemResponseDto dto = new ItemResponseDto();
		dto.setId(22L);
		dto.setTitle("Count Ignored");
		dto.setDescription("Count is not mapped");
		dto.setPrice(12000L);
		dto.setCount(999);

		// when
		ItemEntity entity = itemMapper.toEntity(dto);

		// then
		assertNotNull(entity);
		assertEquals(22L, entity.getId());
		assertEquals("Count Ignored", entity.getTitle());
		assertEquals("Count is not mapped", entity.getDescription());
		assertEquals(12000L, entity.getPrice());
	}
}
