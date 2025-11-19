package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@SpringBootTest
@ActiveProfiles("test")
class CartMapperTest {

	@Autowired
	private CartMapper cartMapper;

	@Test
	void shouldMapToCartItemResponse() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(1L);
		item.setTitle("Smartphone");
		item.setDescription("Great smartphone");
		item.setPrice(25000L);
		item.setImgPath("/images/phone.svg");

		CartEntry entry = new CartEntry(item, 2);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(1L, dto.getId());
		assertEquals("Smartphone", dto.getTitle());
		assertEquals("Great smartphone", dto.getDescription());
		assertEquals(25000L, dto.getPrice());
		assertEquals("/images/phone.svg", dto.getImgPath());
		assertEquals(2, dto.getCount());
	}

	@Test
	void shouldMapToCartItemResponseWithSingleCount() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(2L);
		item.setTitle("Laptop");
		item.setDescription("Powerful laptop");
		item.setPrice(50000L);
		item.setImgPath("/images/laptop.svg");

		CartEntry entry = new CartEntry(item, 1);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(2L, dto.getId());
		assertEquals("Laptop", dto.getTitle());
		assertEquals("Powerful laptop", dto.getDescription());
		assertEquals(50000L, dto.getPrice());
		assertEquals("/images/laptop.svg", dto.getImgPath());
		assertEquals(1, dto.getCount());
	}

	@Test
	void shouldMapToCartItemResponseWithHighCount() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(3L);
		item.setTitle("Headphones");
		item.setDescription("Quality headphones");
		item.setPrice(5000L);
		item.setImgPath("/images/headphones.svg");

		CartEntry entry = new CartEntry(item, 10);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(3L, dto.getId());
		assertEquals("Headphones", dto.getTitle());
		assertEquals("Quality headphones", dto.getDescription());
		assertEquals(5000L, dto.getPrice());
		assertEquals("/images/headphones.svg", dto.getImgPath());
		assertEquals(10, dto.getCount());
	}

	@Test
	void shouldMapToCartItemResponseWithNullDescription() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(4L);
		item.setTitle("Item Without Description");
		item.setDescription(null);
		item.setPrice(1000L);
		item.setImgPath("/images/default.svg");

		CartEntry entry = new CartEntry(item, 1);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(4L, dto.getId());
		assertEquals("Item Without Description", dto.getTitle());
		assertNull(dto.getDescription());
		assertEquals(1000L, dto.getPrice());
		assertEquals("/images/default.svg", dto.getImgPath());
		assertEquals(1, dto.getCount());
	}

	@Test
	void shouldMapToCartItemResponseWithNullImgPath() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(5L);
		item.setTitle("Item Without Image");
		item.setDescription("No image path");
		item.setPrice(2000L);
		item.setImgPath(null);

		CartEntry entry = new CartEntry(item, 3);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(5L, dto.getId());
		assertEquals("Item Without Image", dto.getTitle());
		assertEquals("No image path", dto.getDescription());
		assertEquals(2000L, dto.getPrice());
		assertNull(dto.getImgPath());
		assertEquals(3, dto.getCount());
	}

	@Test
	void shouldMapToCartItemResponseWithZeroPrice() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(6L);
		item.setTitle("Free Item");
		item.setDescription("Free item description");
		item.setPrice(0L);
		item.setImgPath("/images/free.svg");

		CartEntry entry = new CartEntry(item, 1);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(6L, dto.getId());
		assertEquals("Free Item", dto.getTitle());
		assertEquals("Free item description", dto.getDescription());
		assertEquals(0L, dto.getPrice());
		assertEquals("/images/free.svg", dto.getImgPath());
		assertEquals(1, dto.getCount());
	}

	@Test
	void shouldMapToCartItemResponseWithHighPrice() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(7L);
		item.setTitle("Expensive Item");
		item.setDescription("Very expensive item");
		item.setPrice(999999L);
		item.setImgPath("/images/expensive.svg");

		CartEntry entry = new CartEntry(item, 1);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(7L, dto.getId());
		assertEquals("Expensive Item", dto.getTitle());
		assertEquals("Very expensive item", dto.getDescription());
		assertEquals(999999L, dto.getPrice());
		assertEquals("/images/expensive.svg", dto.getImgPath());
		assertEquals(1, dto.getCount());
	}

	@Test
	void shouldMapToCartItemResponseWithAllNullFields() {
		// given
		ItemEntity item = new ItemEntity();
		item.setId(8L);
		item.setTitle("Minimal Item");
		item.setPrice(100L);
		// description and imgPath are null

		CartEntry entry = new CartEntry(item, 1);

		// when
		CartItemResponseDto dto = cartMapper.toCartItemResponse(entry);

		// then
		assertNotNull(dto);
		assertEquals(8L, dto.getId());
		assertEquals("Minimal Item", dto.getTitle());
		assertNull(dto.getDescription());
		assertEquals(100L, dto.getPrice());
		assertNull(dto.getImgPath());
		assertEquals(1, dto.getCount());
	}
}
