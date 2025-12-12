package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.dto.request.OrderDetailsRequestDto;
import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.service.impl.OrderServiceImpl;
import ru.yandex.practicum.mymarket.service.model.CartEntry;
import ru.yandex.practicum.mymarket.service.model.OrderItemModel;
import ru.yandex.practicum.mymarket.service.model.OrderModel;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

	@Autowired
	private OrderService orderService;

	@MockBean
	private OrderRepository orderRepository;

	@MockBean
	private CartService cartService;

	@MockBean
	private OrderMapper orderMapper;

	private ItemEntity testItem1;
	private ItemEntity testItem2;
	private ItemEntity testItem3;
	private OrderEntity testOrder;
	private OrderModel testOrderModel;

	@BeforeEach
	void setUp() {
		// Create test items
		testItem1 = new ItemEntity();
		testItem1.setId(1L);
		testItem1.setTitle("Test Item 1");
		testItem1.setDescription("Description 1");
		testItem1.setPrice(1000L);

		testItem2 = new ItemEntity();
		testItem2.setId(2L);
		testItem2.setTitle("Test Item 2");
		testItem2.setDescription("Description 2");
		testItem2.setPrice(2000L);

		testItem3 = new ItemEntity();
		testItem3.setId(3L);
		testItem3.setTitle("Test Item 3");
		testItem3.setDescription("Description 3");
		testItem3.setPrice(3000L);

		// Create test order entity
		testOrder = new OrderEntity();
		testOrder.setId(1L);
		testOrder.setTotalSum(5000L);
		testOrder.setCreatedAt(LocalDateTime.now());

		OrderItemEntity orderItem1 = new OrderItemEntity("Test Item 1", 1000L, 2);
		OrderItemEntity orderItem2 = new OrderItemEntity("Test Item 2", 2000L, 1);
		testOrder.addItem(orderItem1);
		testOrder.addItem(orderItem2);

		// Create test order model
		List<OrderItemModel> items = Arrays.asList(
				new OrderItemModel("Test Item 1", 1000L, 2),
				new OrderItemModel("Test Item 2", 2000L, 1)
		);
		testOrderModel = new OrderModel(1L, items, 5000L);
	}

	// ==================== Tests for createOrderFromCart() ====================

	@Test
	void shouldCreateOrderFromCartWhenCartHasItems() {
		// given
		List<CartEntry> cartEntries = Arrays.asList(
				new CartEntry(testItem1, 2),
				new CartEntry(testItem2, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);
		savedOrder.setTotalSum(4000L);
		savedOrder.setCreatedAt(LocalDateTime.now());

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		OrderModel result = orderService.createOrderFromCart(cartEntries);

		// then
		assertNotNull(result);
		verify(orderRepository, times(1)).save(any(OrderEntity.class));
		verify(orderMapper, times(1)).toOrderModel(any(OrderEntity.class));

		// Verify the order entity that was saved
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity capturedOrder = orderCaptor.getValue();

		assertEquals(4000L, capturedOrder.getTotalSum());
		assertEquals(2, capturedOrder.getItems().size());
		assertNotNull(capturedOrder.getCreatedAt());
	}

	@Test
	void shouldCalculateTotalSumCorrectly() {
		// given
		List<CartEntry> cartEntries = Arrays.asList(
				new CartEntry(testItem1, 3), // 1000 * 3 = 3000
				new CartEntry(testItem2, 2), // 2000 * 2 = 4000
				new CartEntry(testItem3, 1)  // 3000 * 1 = 3000
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);
		savedOrder.setTotalSum(10000L);

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		orderService.createOrderFromCart(cartEntries);

		// then
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity capturedOrder = orderCaptor.getValue();

		assertEquals(10000L, capturedOrder.getTotalSum());
	}

	@Test
	void shouldSkipItemsWithZeroCount() {
		// given
		List<CartEntry> cartEntries = Arrays.asList(
				new CartEntry(testItem1, 2),
				new CartEntry(testItem2, 0), // should be skipped
				new CartEntry(testItem3, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		orderService.createOrderFromCart(cartEntries);

		// then
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity capturedOrder = orderCaptor.getValue();

		assertEquals(2, capturedOrder.getItems().size());
	}

	@Test
	void shouldSkipItemsWithNegativeCount() {
		// given
		List<CartEntry> cartEntries = Arrays.asList(
				new CartEntry(testItem1, 2),
				new CartEntry(testItem2, -1), // should be skipped
				new CartEntry(testItem3, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		orderService.createOrderFromCart(cartEntries);

		// then
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity capturedOrder = orderCaptor.getValue();

		assertEquals(2, capturedOrder.getItems().size());
		assertEquals(5000L, capturedOrder.getTotalSum()); // 1000*2 + 3000*1 = 2000 + 3000 = 5000
	}

	@Test
	void shouldThrowEmptyCartExceptionWhenCartEntriesIsNull() {
		// when & then
		EmptyCartException exception = assertThrows(EmptyCartException.class, () -> {
			orderService.createOrderFromCart(null);
		});

		assertEquals("Cannot create order from empty cart", exception.getMessage());
		verify(orderRepository, never()).save(any(OrderEntity.class));
	}

	@Test
	void shouldThrowEmptyCartExceptionWhenCartEntriesIsEmpty() {
		// given
		List<CartEntry> emptyCart = Collections.emptyList();

		// when & then
		EmptyCartException exception = assertThrows(EmptyCartException.class, () -> {
			orderService.createOrderFromCart(emptyCart);
		});

		assertEquals("Cannot create order from empty cart", exception.getMessage());
		verify(orderRepository, never()).save(any(OrderEntity.class));
	}

	@Test
	void shouldCreateOrderWithSingleItem() {
		// given
		List<CartEntry> cartEntries = Collections.singletonList(
				new CartEntry(testItem1, 5)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		OrderModel result = orderService.createOrderFromCart(cartEntries);

		// then
		assertNotNull(result);
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity capturedOrder = orderCaptor.getValue();

		assertEquals(1, capturedOrder.getItems().size());
		assertEquals(5000L, capturedOrder.getTotalSum()); // 1000 * 5
	}

	@Test
	void shouldSetCreatedAtTimestamp() {
		// given
		List<CartEntry> cartEntries = Collections.singletonList(
				new CartEntry(testItem1, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		LocalDateTime beforeCreation = LocalDateTime.now();

		// when
		orderService.createOrderFromCart(cartEntries);

		LocalDateTime afterCreation = LocalDateTime.now();

		// then
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity capturedOrder = orderCaptor.getValue();

		assertNotNull(capturedOrder.getCreatedAt());
		assertTrue(capturedOrder.getCreatedAt().isAfter(beforeCreation.minusSeconds(1)));
		assertTrue(capturedOrder.getCreatedAt().isBefore(afterCreation.plusSeconds(1)));
	}

	@Test
	void shouldCreateOrderItemsWithCorrectDetails() {
		// given
		List<CartEntry> cartEntries = Arrays.asList(
				new CartEntry(testItem1, 2),
				new CartEntry(testItem2, 3)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		orderService.createOrderFromCart(cartEntries);

		// then
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		OrderEntity capturedOrder = orderCaptor.getValue();

		assertEquals(2, capturedOrder.getItems().size());

		OrderItemEntity firstItem = capturedOrder.getItems().get(0);
		assertEquals("Test Item 1", firstItem.getTitle());
		assertEquals(1000L, firstItem.getPrice());
		assertEquals(2, firstItem.getCount());

		OrderItemEntity secondItem = capturedOrder.getItems().get(1);
		assertEquals("Test Item 2", secondItem.getTitle());
		assertEquals(2000L, secondItem.getPrice());
		assertEquals(3, secondItem.getCount());
	}

	// ==================== Tests for getOrders() ====================

	@Test
	void shouldGetOrdersWithPagination() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		Page<OrderEntity> orderPage = new PageImpl<>(Collections.singletonList(testOrder), pageable, 1);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(orderPage);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		Page<OrderModel> result = orderService.getOrders(pageable);

		// then
		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getContent().size());
		verify(orderRepository, times(1)).findAllWithItems(pageable);
		verify(orderMapper, times(1)).toOrderModel(any(OrderEntity.class));
	}

	@Test
	void shouldGetEmptyPageWhenNoOrders() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		Page<OrderEntity> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(emptyPage);

		// when
		Page<OrderModel> result = orderService.getOrders(pageable);

		// then
		assertNotNull(result);
		assertEquals(0, result.getTotalElements());
		assertTrue(result.getContent().isEmpty());
		verify(orderRepository, times(1)).findAllWithItems(pageable);
		verify(orderMapper, never()).toOrderModel(any(OrderEntity.class));
	}

	@Test
	void shouldGetOrdersWithMultiplePages() {
		// given
		Pageable pageable = PageRequest.of(1, 5);
		List<OrderEntity> orders = Arrays.asList(testOrder, testOrder, testOrder);
		Page<OrderEntity> orderPage = new PageImpl<>(orders, pageable, 20);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(orderPage);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		Page<OrderModel> result = orderService.getOrders(pageable);

		// then
		assertNotNull(result);
		assertEquals(20, result.getTotalElements());
		assertEquals(3, result.getContent().size());
		assertTrue(result.hasNext());
		assertTrue(result.hasPrevious());
		verify(orderMapper, times(3)).toOrderModel(any(OrderEntity.class));
	}

	@Test
	void shouldGetOrdersWithDifferentPageSizes() {
		// given
		Pageable pageable = PageRequest.of(0, 20);
		Page<OrderEntity> orderPage = new PageImpl<>(Collections.singletonList(testOrder), pageable, 1);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(orderPage);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		Page<OrderModel> result = orderService.getOrders(pageable);

		// then
		assertNotNull(result);
		assertEquals(20, result.getSize());
		verify(orderRepository, times(1)).findAllWithItems(pageable);
	}

	// ==================== Tests for getOrder() ====================

	@Test
	void shouldGetOrderWhenOrderExists() {
		// given
		Long orderId = 1L;
		when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));
		when(orderMapper.toOrderModel(testOrder)).thenReturn(testOrderModel);

		// when
		Optional<OrderModel> result = orderService.getOrder(orderId);

		// then
		assertTrue(result.isPresent());
		assertEquals(testOrderModel, result.get());
		verify(orderRepository, times(1)).findByIdWithItems(orderId);
		verify(orderMapper, times(1)).toOrderModel(testOrder);
	}

	@Test
	void shouldReturnEmptyWhenOrderNotFound() {
		// given
		Long orderId = 999L;
		when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.empty());

		// when
		Optional<OrderModel> result = orderService.getOrder(orderId);

		// then
		assertFalse(result.isPresent());
		verify(orderRepository, times(1)).findByIdWithItems(orderId);
		verify(orderMapper, never()).toOrderModel(any(OrderEntity.class));
	}

	@Test
	void shouldGetOrderWithMultipleItems() {
		// given
		Long orderId = 1L;
		OrderEntity orderWithManyItems = new OrderEntity();
		orderWithManyItems.setId(orderId);
		orderWithManyItems.setTotalSum(10000L);
		orderWithManyItems.addItem(new OrderItemEntity("Item 1", 1000L, 1));
		orderWithManyItems.addItem(new OrderItemEntity("Item 2", 2000L, 2));
		orderWithManyItems.addItem(new OrderItemEntity("Item 3", 3000L, 1));

		when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(orderWithManyItems));
		when(orderMapper.toOrderModel(orderWithManyItems)).thenReturn(testOrderModel);

		// when
		Optional<OrderModel> result = orderService.getOrder(orderId);

		// then
		assertTrue(result.isPresent());
		verify(orderRepository, times(1)).findByIdWithItems(orderId);
	}

	// ==================== Tests for buy() ====================

	@Test
	void shouldBuyWhenCartHasItems() {
		// given
		List<CartEntry> cartEntries = Arrays.asList(
				new CartEntry(testItem1, 2),
				new CartEntry(testItem2, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(5L);

		when(cartService.getItems()).thenReturn(cartEntries);
		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(
				new OrderModel(5L, new ArrayList<>(), 4000L)
		);

		// when
		String redirect = orderService.buy();

		// then
		assertEquals("redirect:/orders/5?newOrder=true", redirect);
		verify(cartService, times(1)).getItems();
		verify(cartService, times(1)).clear();
		verify(orderRepository, times(1)).save(any(OrderEntity.class));
	}

	@Test
	void shouldThrowEmptyCartExceptionWhenBuyingWithEmptyCart() {
		// given
		when(cartService.getItems()).thenReturn(Collections.emptyList());

		// when & then
		EmptyCartException exception = assertThrows(EmptyCartException.class, () -> {
			orderService.buy();
		});

		assertEquals("Cannot create order from empty cart", exception.getMessage());
		verify(cartService, times(1)).getItems();
		verify(cartService, never()).clear();
		verify(orderRepository, never()).save(any(OrderEntity.class));
	}

	@Test
	void shouldClearCartAfterSuccessfulBuy() {
		// given
		List<CartEntry> cartEntries = Collections.singletonList(
				new CartEntry(testItem1, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(1L);

		when(cartService.getItems()).thenReturn(cartEntries);
		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		orderService.buy();

		// then
		verify(cartService, times(1)).clear();
	}

	@Test
	void shouldReturnCorrectRedirectUrlWithOrderId() {
		// given
		List<CartEntry> cartEntries = Collections.singletonList(
				new CartEntry(testItem1, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(42L);

		when(cartService.getItems()).thenReturn(cartEntries);
		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(
				new OrderModel(42L, new ArrayList<>(), 1000L)
		);

		// when
		String redirect = orderService.buy();

		// then
		assertTrue(redirect.contains("redirect:/orders/42"));
		assertTrue(redirect.contains("newOrder=true"));
	}

	// ==================== Tests for showOrders() ====================

	@Test
	void shouldShowOrdersWithCorrectModelAttributes() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		List<OrderEntity> orders = Collections.singletonList(testOrder);
		Page<OrderEntity> orderPage = new PageImpl<>(orders, pageable, 1);

		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 5000L);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(orderPage);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);
		when(orderMapper.toOrderResponse(any(OrderModel.class))).thenReturn(orderResponseDto);

		Model model = new ExtendedModelMap();

		// when
		String viewName = orderService.showOrders(pageable, model);

		// then
		assertEquals("orders", viewName);

		@SuppressWarnings("unchecked")
		List<OrderResponseDto> orders_attr = (List<OrderResponseDto>) model.getAttribute("orders");
		assertNotNull(orders_attr);
		assertEquals(1, orders_attr.size());

		assertEquals(1, model.getAttribute("pageNumber"));
		assertEquals(10, model.getAttribute("pageSize"));
		assertEquals(1, model.getAttribute("totalPages"));
		assertEquals(false, model.getAttribute("hasNext"));
		assertEquals(false, model.getAttribute("hasPrevious"));
	}

	@Test
	void shouldShowOrdersWithEmptyList() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		Page<OrderEntity> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(emptyPage);

		Model model = new ExtendedModelMap();

		// when
		String viewName = orderService.showOrders(pageable, model);

		// then
		assertEquals("orders", viewName);

		@SuppressWarnings("unchecked")
		List<OrderResponseDto> orders = (List<OrderResponseDto>) model.getAttribute("orders");
		assertNotNull(orders);
		assertTrue(orders.isEmpty());
	}

	@Test
	void shouldShowOrdersWithPaginationInfo() {
		// given
		Pageable pageable = PageRequest.of(1, 5);
		List<OrderEntity> orders = Arrays.asList(testOrder, testOrder, testOrder, testOrder, testOrder);
		Page<OrderEntity> orderPage = new PageImpl<>(orders, pageable, 15);

		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 5000L);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(orderPage);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);
		when(orderMapper.toOrderResponse(any(OrderModel.class))).thenReturn(orderResponseDto);

		Model model = new ExtendedModelMap();

		// when
		String viewName = orderService.showOrders(pageable, model);

		// then
		assertEquals("orders", viewName);
		assertEquals(2, model.getAttribute("pageNumber")); // pageNumber + 1
		assertEquals(5, model.getAttribute("pageSize"));
		assertEquals(3, model.getAttribute("totalPages")); // 15 / 5
		assertEquals(true, model.getAttribute("hasNext"));
		assertEquals(true, model.getAttribute("hasPrevious"));
	}

	@Test
	void shouldShowOrdersOnLastPage() {
		// given
		Pageable pageable = PageRequest.of(2, 5);
		List<OrderEntity> orders = Collections.singletonList(testOrder);
		Page<OrderEntity> orderPage = new PageImpl<>(orders, pageable, 11);

		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 5000L);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(orderPage);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);
		when(orderMapper.toOrderResponse(any(OrderModel.class))).thenReturn(orderResponseDto);

		Model model = new ExtendedModelMap();

		// when
		String viewName = orderService.showOrders(pageable, model);

		// then
		assertEquals("orders", viewName);
		assertEquals(3, model.getAttribute("pageNumber"));
		assertEquals(false, model.getAttribute("hasNext"));
		assertEquals(true, model.getAttribute("hasPrevious"));
	}

	@Test
	void shouldCallMapperForEachOrder() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		List<OrderEntity> orders = Arrays.asList(testOrder, testOrder, testOrder);
		Page<OrderEntity> orderPage = new PageImpl<>(orders, pageable, 3);

		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 5000L);

		when(orderRepository.findAllWithItems(pageable)).thenReturn(orderPage);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);
		when(orderMapper.toOrderResponse(any(OrderModel.class))).thenReturn(orderResponseDto);

		Model model = new ExtendedModelMap();

		// when
		orderService.showOrders(pageable, model);

		// then
		verify(orderMapper, times(3)).toOrderModel(any(OrderEntity.class));
		verify(orderMapper, times(3)).toOrderResponse(any(OrderModel.class));
	}

	// ==================== Tests for showOrder() ====================

	@Test
	void shouldShowOrderWhenOrderExists() {
		// given
		OrderDetailsRequestDto request = new OrderDetailsRequestDto();
		request.setId(1L);
		request.setNewOrder(true);

		List<OrderItemModel> items = Arrays.asList(
				new OrderItemModel("Test Item 1", 1000L, 2),
				new OrderItemModel("Test Item 2", 2000L, 1)
		);
		OrderModel orderModel = new OrderModel(1L, items, 4000L);

		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 4000L);
		OrderItemResponseDto itemResponseDto1 = new OrderItemResponseDto("Test Item 1", 1000L, 2);
		OrderItemResponseDto itemResponseDto2 = new OrderItemResponseDto("Test Item 2", 2000L, 1);

		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));
		when(orderMapper.toOrderModel(testOrder)).thenReturn(orderModel);
		when(orderMapper.toOrderResponse(orderModel)).thenReturn(orderResponseDto);
		when(orderMapper.toOrderItemResponse(items.get(0))).thenReturn(itemResponseDto1);
		when(orderMapper.toOrderItemResponse(items.get(1))).thenReturn(itemResponseDto2);

		Model model = new ExtendedModelMap();

		// when
		String viewName = orderService.showOrder(request, model);

		// then
		assertEquals("order", viewName);
		assertNotNull(model.getAttribute("order"));
		assertNotNull(model.getAttribute("items"));
		assertEquals(true, model.getAttribute("newOrder"));

		@SuppressWarnings("unchecked")
		List<OrderItemResponseDto> itemsList = (List<OrderItemResponseDto>) model.getAttribute("items");
		assertEquals(2, itemsList.size());
	}

	@Test
	void shouldShowOrderWithNewOrderFalse() {
		// given
		OrderDetailsRequestDto request = new OrderDetailsRequestDto();
		request.setId(1L);
		request.setNewOrder(false);

		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 4000L);

		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));
		when(orderMapper.toOrderModel(testOrder)).thenReturn(testOrderModel);
		when(orderMapper.toOrderResponse(testOrderModel)).thenReturn(orderResponseDto);

		Model model = new ExtendedModelMap();

		// when
		String viewName = orderService.showOrder(request, model);

		// then
		assertEquals("order", viewName);
		assertEquals(false, model.getAttribute("newOrder"));
	}

	@Test
	void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
		// given
		OrderDetailsRequestDto request = new OrderDetailsRequestDto();
		request.setId(999L);

		when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

		Model model = new ExtendedModelMap();

		// when & then
		OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
			orderService.showOrder(request, model);
		});

		assertTrue(exception.getMessage().contains("Order not found with id: 999"));
		verify(orderMapper, never()).toOrderResponse(any(OrderModel.class));
	}

	@Test
	void shouldShowOrderWithEmptyItemsList() {
		// given
		OrderDetailsRequestDto request = new OrderDetailsRequestDto();
		request.setId(1L);

		OrderModel orderModelWithNoItems = new OrderModel(1L, Collections.emptyList(), 0L);
		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 0L);

		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));
		when(orderMapper.toOrderModel(testOrder)).thenReturn(orderModelWithNoItems);
		when(orderMapper.toOrderResponse(orderModelWithNoItems)).thenReturn(orderResponseDto);

		Model model = new ExtendedModelMap();

		// when
		String viewName = orderService.showOrder(request, model);

		// then
		assertEquals("order", viewName);
		@SuppressWarnings("unchecked")
		List<OrderItemResponseDto> items = (List<OrderItemResponseDto>) model.getAttribute("items");
		assertNotNull(items);
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldCallMapperForEachOrderItem() {
		// given
		OrderDetailsRequestDto request = new OrderDetailsRequestDto();
		request.setId(1L);

		List<OrderItemModel> items = Arrays.asList(
				new OrderItemModel("Item 1", 1000L, 1),
				new OrderItemModel("Item 2", 2000L, 2),
				new OrderItemModel("Item 3", 3000L, 3)
		);
		OrderModel orderModel = new OrderModel(1L, items, 14000L);

		OrderResponseDto orderResponseDto = new OrderResponseDto(1L, new ArrayList<>(), 14000L);
		OrderItemResponseDto itemResponseDto = new OrderItemResponseDto();

		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));
		when(orderMapper.toOrderModel(testOrder)).thenReturn(orderModel);
		when(orderMapper.toOrderResponse(orderModel)).thenReturn(orderResponseDto);
		when(orderMapper.toOrderItemResponse(any(OrderItemModel.class))).thenReturn(itemResponseDto);

		Model model = new ExtendedModelMap();

		// when
		orderService.showOrder(request, model);

		// then
		verify(orderMapper, times(3)).toOrderItemResponse(any(OrderItemModel.class));
	}

	@Test
	void shouldShowOrderWithDifferentOrderIds() {
		// given
		OrderDetailsRequestDto request1 = new OrderDetailsRequestDto();
		request1.setId(1L);

		OrderDetailsRequestDto request2 = new OrderDetailsRequestDto();
		request2.setId(2L);

		OrderEntity order1 = new OrderEntity();
		order1.setId(1L);
		order1.setTotalSum(1000L);

		OrderEntity order2 = new OrderEntity();
		order2.setId(2L);
		order2.setTotalSum(2000L);

		OrderModel orderModel1 = new OrderModel(1L, new ArrayList<>(), 1000L);
		OrderModel orderModel2 = new OrderModel(2L, new ArrayList<>(), 2000L);

		OrderResponseDto orderResponseDto1 = new OrderResponseDto(1L, new ArrayList<>(), 1000L);
		OrderResponseDto orderResponseDto2 = new OrderResponseDto(2L, new ArrayList<>(), 2000L);

		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order1));
		when(orderRepository.findByIdWithItems(2L)).thenReturn(Optional.of(order2));
		when(orderMapper.toOrderModel(order1)).thenReturn(orderModel1);
		when(orderMapper.toOrderModel(order2)).thenReturn(orderModel2);
		when(orderMapper.toOrderResponse(orderModel1)).thenReturn(orderResponseDto1);
		when(orderMapper.toOrderResponse(orderModel2)).thenReturn(orderResponseDto2);

		Model model1 = new ExtendedModelMap();
		Model model2 = new ExtendedModelMap();

		// when
		String viewName1 = orderService.showOrder(request1, model1);
		String viewName2 = orderService.showOrder(request2, model2);

		// then
		assertEquals("order", viewName1);
		assertEquals("order", viewName2);
		verify(orderRepository, times(1)).findByIdWithItems(1L);
		verify(orderRepository, times(1)).findByIdWithItems(2L);
	}

	// ==================== Integration Tests ====================

	@Test
	void shouldHandleCompleteOrderFlow() {
		// given - add items to cart
		List<CartEntry> cartEntries = Arrays.asList(
				new CartEntry(testItem1, 2),
				new CartEntry(testItem2, 1)
		);

		OrderEntity savedOrder = new OrderEntity();
		savedOrder.setId(10L);
		savedOrder.setTotalSum(4000L);

		when(cartService.getItems()).thenReturn(cartEntries);
		when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(
				new OrderModel(10L, new ArrayList<>(), 4000L)
		);

		// when - buy
		String redirect = orderService.buy();

		// then - order created and cart cleared
		assertTrue(redirect.contains("redirect:/orders/10"));
		verify(cartService, times(1)).clear();
		verify(orderRepository, times(1)).save(any(OrderEntity.class));
	}

	@Test
	void shouldHandleMultipleOrderCreations() {
		// given
		List<CartEntry> cartEntries1 = Collections.singletonList(new CartEntry(testItem1, 1));
		List<CartEntry> cartEntries2 = Collections.singletonList(new CartEntry(testItem2, 2));

		OrderEntity order1 = new OrderEntity();
		order1.setId(1L);
		OrderEntity order2 = new OrderEntity();
		order2.setId(2L);

		when(orderRepository.save(any(OrderEntity.class))).thenReturn(order1, order2);
		when(orderMapper.toOrderModel(any(OrderEntity.class))).thenReturn(testOrderModel);

		// when
		OrderModel result1 = orderService.createOrderFromCart(cartEntries1);
		OrderModel result2 = orderService.createOrderFromCart(cartEntries2);

		// then
		assertNotNull(result1);
		assertNotNull(result2);
		verify(orderRepository, times(2)).save(any(OrderEntity.class));
	}

	@Test
	void shouldMaintainOrderStateConsistency() {
		// given
		Long orderId = 1L;
		when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));
		when(orderMapper.toOrderModel(testOrder)).thenReturn(testOrderModel);

		// when - get order multiple times
		Optional<OrderModel> result1 = orderService.getOrder(orderId);
		Optional<OrderModel> result2 = orderService.getOrder(orderId);

		// then - should return consistent results
		assertTrue(result1.isPresent());
		assertTrue(result2.isPresent());
		assertEquals(result1.get().getId(), result2.get().getId());
		verify(orderRepository, times(2)).findByIdWithItems(orderId);
	}
}
