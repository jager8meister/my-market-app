package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;
import ru.yandex.practicum.mymarket.service.model.OrderItemModel;
import ru.yandex.practicum.mymarket.service.model.OrderModel;

@SpringBootTest
@ActiveProfiles("test")
class OrderMapperTest {

	@Autowired
	private OrderMapper orderMapper;

	@Test
	void shouldMapToOrderItemResponse() {
		// given
		OrderItemModel model = new OrderItemModel("Smartphone", 25000L, 2);

		// when
		OrderItemResponseDto dto = orderMapper.toOrderItemResponse(model);

		// then
		assertNotNull(dto);
		assertEquals("Smartphone", dto.getTitle());
		assertEquals(25000L, dto.getPrice());
		assertEquals(2, dto.getCount());
	}

	@Test
	void shouldMapToOrderItemResponseWithSingleCount() {
		// given
		OrderItemModel model = new OrderItemModel("Laptop", 50000L, 1);

		// when
		OrderItemResponseDto dto = orderMapper.toOrderItemResponse(model);

		// then
		assertNotNull(dto);
		assertEquals("Laptop", dto.getTitle());
		assertEquals(50000L, dto.getPrice());
		assertEquals(1, dto.getCount());
	}

	@Test
	void shouldMapToOrderItemResponseWithHighCount() {
		// given
		OrderItemModel model = new OrderItemModel("Headphones", 5000L, 10);

		// when
		OrderItemResponseDto dto = orderMapper.toOrderItemResponse(model);

		// then
		assertNotNull(dto);
		assertEquals("Headphones", dto.getTitle());
		assertEquals(5000L, dto.getPrice());
		assertEquals(10, dto.getCount());
	}

	@Test
	void shouldMapToOrderItemResponseWithZeroPrice() {
		// given
		OrderItemModel model = new OrderItemModel("Free Item", 0L, 1);

		// when
		OrderItemResponseDto dto = orderMapper.toOrderItemResponse(model);

		// then
		assertNotNull(dto);
		assertEquals("Free Item", dto.getTitle());
		assertEquals(0L, dto.getPrice());
		assertEquals(1, dto.getCount());
	}

	@Test
	void shouldMapToOrderItemResponseList() {
		// given
		List<OrderItemModel> models = Arrays.asList(
			new OrderItemModel("Item 1", 1000L, 1),
			new OrderItemModel("Item 2", 2000L, 2),
			new OrderItemModel("Item 3", 3000L, 3)
		);

		// when
		List<OrderItemResponseDto> dtos = orderMapper.toOrderItemResponseList(models);

		// then
		assertNotNull(dtos);
		assertEquals(3, dtos.size());

		assertEquals("Item 1", dtos.get(0).getTitle());
		assertEquals(1000L, dtos.get(0).getPrice());
		assertEquals(1, dtos.get(0).getCount());

		assertEquals("Item 2", dtos.get(1).getTitle());
		assertEquals(2000L, dtos.get(1).getPrice());
		assertEquals(2, dtos.get(1).getCount());

		assertEquals("Item 3", dtos.get(2).getTitle());
		assertEquals(3000L, dtos.get(2).getPrice());
		assertEquals(3, dtos.get(2).getCount());
	}

	@Test
	void shouldMapToOrderItemResponseListWithEmptyList() {
		// given
		List<OrderItemModel> models = Collections.emptyList();

		// when
		List<OrderItemResponseDto> dtos = orderMapper.toOrderItemResponseList(models);

		// then
		assertNotNull(dtos);
		assertTrue(dtos.isEmpty());
	}

	@Test
	void shouldMapToOrderItemResponseListWithSingleItem() {
		// given
		List<OrderItemModel> models = Collections.singletonList(
			new OrderItemModel("Single Item", 12000L, 1)
		);

		// when
		List<OrderItemResponseDto> dtos = orderMapper.toOrderItemResponseList(models);

		// then
		assertNotNull(dtos);
		assertEquals(1, dtos.size());
		assertEquals("Single Item", dtos.get(0).getTitle());
		assertEquals(12000L, dtos.get(0).getPrice());
		assertEquals(1, dtos.get(0).getCount());
	}

	@Test
	void shouldMapToOrderResponse() {
		// given
		List<OrderItemModel> items = Arrays.asList(
			new OrderItemModel("Smartphone", 25000L, 2),
			new OrderItemModel("Headphones", 5000L, 1)
		);
		OrderModel model = new OrderModel(1L, items, 55000L);

		// when
		OrderResponseDto dto = orderMapper.toOrderResponse(model);

		// then
		assertNotNull(dto);
		assertEquals(1L, dto.getId());
		assertEquals(55000L, dto.getTotalSum());
		assertNotNull(dto.getItems());
		assertEquals(2, dto.getItems().size());

		assertEquals("Smartphone", dto.getItems().get(0).getTitle());
		assertEquals(25000L, dto.getItems().get(0).getPrice());
		assertEquals(2, dto.getItems().get(0).getCount());

		assertEquals("Headphones", dto.getItems().get(1).getTitle());
		assertEquals(5000L, dto.getItems().get(1).getPrice());
		assertEquals(1, dto.getItems().get(1).getCount());
	}

	@Test
	void shouldMapToOrderResponseWithEmptyItems() {
		// given
		OrderModel model = new OrderModel(2L, Collections.emptyList(), 0L);

		// when
		OrderResponseDto dto = orderMapper.toOrderResponse(model);

		// then
		assertNotNull(dto);
		assertEquals(2L, dto.getId());
		assertEquals(0L, dto.getTotalSum());
		assertNotNull(dto.getItems());
		assertTrue(dto.getItems().isEmpty());
	}

	@Test
	void shouldMapToOrderResponseWithSingleItem() {
		// given
		List<OrderItemModel> items = Collections.singletonList(
			new OrderItemModel("Only Item", 15000L, 1)
		);
		OrderModel model = new OrderModel(3L, items, 15000L);

		// when
		OrderResponseDto dto = orderMapper.toOrderResponse(model);

		// then
		assertNotNull(dto);
		assertEquals(3L, dto.getId());
		assertEquals(15000L, dto.getTotalSum());
		assertNotNull(dto.getItems());
		assertEquals(1, dto.getItems().size());
		assertEquals("Only Item", dto.getItems().get(0).getTitle());
	}

	@Test
	void shouldMapToOrderResponseWithMultipleItems() {
		// given
		List<OrderItemModel> items = new ArrayList<>();
		items.add(new OrderItemModel("Item A", 1000L, 1));
		items.add(new OrderItemModel("Item B", 2000L, 2));
		items.add(new OrderItemModel("Item C", 3000L, 3));
		items.add(new OrderItemModel("Item D", 4000L, 4));
		OrderModel model = new OrderModel(4L, items, 30000L);

		// when
		OrderResponseDto dto = orderMapper.toOrderResponse(model);

		// then
		assertNotNull(dto);
		assertEquals(4L, dto.getId());
		assertEquals(30000L, dto.getTotalSum());
		assertNotNull(dto.getItems());
		assertEquals(4, dto.getItems().size());
	}

	@Test
	void shouldMapToOrderResponseWithZeroTotalSum() {
		// given
		List<OrderItemModel> items = Collections.singletonList(
			new OrderItemModel("Free Item", 0L, 5)
		);
		OrderModel model = new OrderModel(5L, items, 0L);

		// when
		OrderResponseDto dto = orderMapper.toOrderResponse(model);

		// then
		assertNotNull(dto);
		assertEquals(5L, dto.getId());
		assertEquals(0L, dto.getTotalSum());
		assertNotNull(dto.getItems());
		assertEquals(1, dto.getItems().size());
	}

	@Test
	void shouldMapToOrderResponseWithHighTotalSum() {
		// given
		List<OrderItemModel> items = Arrays.asList(
			new OrderItemModel("Expensive Item 1", 500000L, 1),
			new OrderItemModel("Expensive Item 2", 300000L, 2)
		);
		OrderModel model = new OrderModel(6L, items, 1100000L);

		// when
		OrderResponseDto dto = orderMapper.toOrderResponse(model);

		// then
		assertNotNull(dto);
		assertEquals(6L, dto.getId());
		assertEquals(1100000L, dto.getTotalSum());
		assertNotNull(dto.getItems());
		assertEquals(2, dto.getItems().size());
	}
}
