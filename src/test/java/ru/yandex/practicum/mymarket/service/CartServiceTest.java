package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.enums.SortType;
import ru.yandex.practicum.mymarket.mapper.CartMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceTest {

	@Autowired
	private CartService cartService;

	@Autowired
	private ItemRepository itemRepository;

	@MockBean
	private CartMapper cartMapper;

	private ItemEntity testItem1;
	private ItemEntity testItem2;
	private ItemEntity testItem3;

	@BeforeEach
	void setUp() {
		cartService.clear();

		testItem1 = new ItemEntity();
		testItem1.setTitle("Test Item 1");
		testItem1.setDescription("Test Description 1");
		testItem1.setPrice(1000L);
		testItem1 = itemRepository.save(testItem1);

		testItem2 = new ItemEntity();
		testItem2.setTitle("Test Item 2");
		testItem2.setDescription("Test Description 2");
		testItem2.setPrice(2000L);
		testItem2 = itemRepository.save(testItem2);

		testItem3 = new ItemEntity();
		testItem3.setTitle("Test Item 3");
		testItem3.setDescription("Test Description 3");
		testItem3.setPrice(3000L);
		testItem3 = itemRepository.save(testItem3);
	}

	// ==================== Tests for addItem() ====================

	@Test
	void shouldAddItemToCartWhenItemExists() {
		// given
		Long itemId = testItem1.getId();

		// when
		cartService.addItem(itemId);

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(itemId, items.get(0).getItem().getId());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void shouldIncreaseItemCountWhenAddingSameItemTwice() {
		// given
		Long itemId = testItem1.getId();

		// when
		cartService.addItem(itemId);
		cartService.addItem(itemId);

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(2, items.get(0).getCount());
	}

	@Test
	void shouldIncreaseItemCountWhenAddingSameItemMultipleTimes() {
		// given
		Long itemId = testItem1.getId();

		// when
		cartService.addItem(itemId);
		cartService.addItem(itemId);
		cartService.addItem(itemId);
		cartService.addItem(itemId);
		cartService.addItem(itemId);

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(5, items.get(0).getCount());
	}

	@Test
	void shouldAddMultipleDifferentItemsToCart() {
		// when
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());
		cartService.addItem(testItem3.getId());

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(3, items.size());
	}

	@Test
	void shouldNotAddItemWhenItemIdIsNull() {
		// when
		cartService.addItem(null);

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldNotAddItemWhenItemDoesNotExist() {
		// when
		cartService.addItem(999999L);

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	// ==================== Tests for removeOne() ====================

	@Test
	void shouldRemoveOneItemFromCartWhenMultipleExist() {
		// given
		Long itemId = testItem1.getId();
		cartService.addItem(itemId);
		cartService.addItem(itemId);
		cartService.addItem(itemId);

		// when
		cartService.removeOne(itemId);

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(2, items.get(0).getCount());
	}

	@Test
	void shouldRemoveItemCompletelyWhenRemovingLastOne() {
		// given
		Long itemId = testItem1.getId();
		cartService.addItem(itemId);

		// when
		cartService.removeOne(itemId);

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldNotFailWhenRemovingOneFromEmptyCart() {
		// when
		cartService.removeOne(testItem1.getId());

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldNotFailWhenRemovingOneWithNullItemId() {
		// when
		cartService.removeOne(null);

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldNotFailWhenRemovingNonExistentItem() {
		// given
		cartService.addItem(testItem1.getId());

		// when
		cartService.removeOne(999999L);

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
	}

	@Test
	void shouldNotFailWhenRemovingOneFromItemWithZeroCount() {
		// This tests the edge case where count is 0 or negative
		// when
		cartService.removeOne(testItem1.getId());

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	// ==================== Tests for removeAll() ====================

	@Test
	void shouldRemoveAllItemsOfSameType() {
		// given
		Long itemId = testItem1.getId();
		cartService.addItem(itemId);
		cartService.addItem(itemId);
		cartService.addItem(itemId);

		// when
		cartService.removeAll(itemId);

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldRemoveAllOnlySpecificItemType() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());
		cartService.addItem(testItem3.getId());

		// when
		cartService.removeAll(testItem1.getId());

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(2, items.size());
		assertFalse(items.stream().anyMatch(item -> item.getItem().getId().equals(testItem1.getId())));
	}

	@Test
	void shouldNotFailWhenRemovingAllWithNullItemId() {
		// given
		cartService.addItem(testItem1.getId());

		// when
		cartService.removeAll(null);

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
	}

	@Test
	void shouldNotFailWhenRemovingAllFromEmptyCart() {
		// when
		cartService.removeAll(testItem1.getId());

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldNotFailWhenRemovingAllNonExistentItem() {
		// given
		cartService.addItem(testItem1.getId());

		// when
		cartService.removeAll(999999L);

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
	}

	// ==================== Tests for getItems() ====================

	@Test
	void shouldReturnEmptyListWhenCartIsEmpty() {
		// when
		List<CartEntry> items = cartService.getItems();

		// then
		assertNotNull(items);
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldReturnAllItemsInCart() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());
		cartService.addItem(testItem3.getId());

		// when
		List<CartEntry> items = cartService.getItems();

		// then
		assertEquals(3, items.size());
	}

	@Test
	void shouldReturnItemsWithCorrectCounts() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());

		// when
		List<CartEntry> items = cartService.getItems();

		// then
		assertEquals(2, items.size());
		CartEntry item1Entry = items.stream()
				.filter(entry -> entry.getItem().getId().equals(testItem1.getId()))
				.findFirst()
				.orElse(null);
		assertNotNull(item1Entry);
		assertEquals(2, item1Entry.getCount());
	}

	@Test
	void shouldReturnItemsWithCorrectItemDetails() {
		// given
		cartService.addItem(testItem1.getId());

		// when
		List<CartEntry> items = cartService.getItems();

		// then
		assertEquals(1, items.size());
		CartEntry entry = items.get(0);
		assertEquals(testItem1.getId(), entry.getItem().getId());
		assertEquals(testItem1.getTitle(), entry.getItem().getTitle());
		assertEquals(testItem1.getPrice(), entry.getItem().getPrice());
	}

	// ==================== Tests for getTotalPrice() ====================

	@Test
	void shouldReturnZeroWhenCartIsEmpty() {
		// when
		long totalPrice = cartService.getTotalPrice();

		// then
		assertEquals(0L, totalPrice);
	}

	@Test
	void shouldCalculateTotalPriceForSingleItem() {
		// given
		cartService.addItem(testItem1.getId());

		// when
		long totalPrice = cartService.getTotalPrice();

		// then
		assertEquals(1000L, totalPrice);
	}

	@Test
	void shouldCalculateTotalPriceForMultipleSameItems() {
		// given
		Long itemId = testItem1.getId();
		cartService.addItem(itemId);
		cartService.addItem(itemId);

		// when
		long totalPrice = cartService.getTotalPrice();

		// then
		assertEquals(2000L, totalPrice);
	}

	@Test
	void shouldCalculateTotalPriceForMultipleDifferentItems() {
		// given
		cartService.addItem(testItem1.getId()); // 1000
		cartService.addItem(testItem2.getId()); // 2000
		cartService.addItem(testItem3.getId()); // 3000

		// when
		long totalPrice = cartService.getTotalPrice();

		// then
		assertEquals(6000L, totalPrice);
	}

	@Test
	void shouldCalculateTotalPriceForMixedItems() {
		// given
		cartService.addItem(testItem1.getId()); // 1000
		cartService.addItem(testItem1.getId()); // 1000
		cartService.addItem(testItem2.getId()); // 2000
		cartService.addItem(testItem3.getId()); // 3000
		cartService.addItem(testItem3.getId()); // 3000
		cartService.addItem(testItem3.getId()); // 3000

		// when
		long totalPrice = cartService.getTotalPrice();

		// then
		// 2 * 1000 + 1 * 2000 + 3 * 3000 = 2000 + 2000 + 9000 = 13000
		assertEquals(13000L, totalPrice);
	}

	// ==================== Tests for clear() ====================

	@Test
	void shouldClearEmptyCart() {
		// when
		cartService.clear();

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
		assertEquals(0, cartService.getTotalPrice());
	}

	@Test
	void shouldClearCartWithSingleItem() {
		// given
		cartService.addItem(testItem1.getId());

		// when
		cartService.clear();

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
		assertEquals(0, cartService.getTotalPrice());
	}

	@Test
	void shouldClearCartWithMultipleItems() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());
		cartService.addItem(testItem3.getId());

		// when
		cartService.clear();

		// then
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
		assertEquals(0, cartService.getTotalPrice());
	}

	@Test
	void shouldAllowAddingItemsAfterClear() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.clear();

		// when
		cartService.addItem(testItem2.getId());

		// then
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(testItem2.getId(), items.get(0).getItem().getId());
	}

	// ==================== Tests for showCart() ====================

	@Test
	void shouldShowEmptyCart() {
		// given
		Model model = new ExtendedModelMap();
		when(cartMapper.toCartItemResponse(any(CartEntry.class))).thenReturn(new CartItemResponseDto());

		// when
		String viewName = cartService.showCart(model);

		// then
		assertEquals("cart", viewName);
		@SuppressWarnings("unchecked")
		List<CartItemResponseDto> items = (List<CartItemResponseDto>) model.getAttribute("items");
		assertNotNull(items);
		assertTrue(items.isEmpty());
		assertEquals(0L, model.getAttribute("total"));
	}

	@Test
	void shouldShowCartWithItems() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());

		CartItemResponseDto dto1 = new CartItemResponseDto();
		dto1.setId(testItem1.getId());
		dto1.setTitle(testItem1.getTitle());
		dto1.setPrice(testItem1.getPrice());
		dto1.setCount(1);

		CartItemResponseDto dto2 = new CartItemResponseDto();
		dto2.setId(testItem2.getId());
		dto2.setTitle(testItem2.getTitle());
		dto2.setPrice(testItem2.getPrice());
		dto2.setCount(1);

		when(cartMapper.toCartItemResponse(any(CartEntry.class)))
				.thenReturn(dto1, dto2);

		Model model = new ExtendedModelMap();

		// when
		String viewName = cartService.showCart(model);

		// then
		assertEquals("cart", viewName);
		@SuppressWarnings("unchecked")
		List<CartItemResponseDto> items = (List<CartItemResponseDto>) model.getAttribute("items");
		assertNotNull(items);
		assertEquals(2, items.size());
		assertEquals(3000L, model.getAttribute("total")); // 1000 + 2000
	}

	@Test
	void shouldShowCartWithCorrectTotal() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());

		CartItemResponseDto dto = new CartItemResponseDto();
		when(cartMapper.toCartItemResponse(any(CartEntry.class))).thenReturn(dto);

		Model model = new ExtendedModelMap();

		// when
		String viewName = cartService.showCart(model);

		// then
		assertEquals("cart", viewName);
		assertEquals(4000L, model.getAttribute("total")); // 2 * 1000 + 1 * 2000
	}

	@Test
	void shouldCallCartMapperForEachItem() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());

		CartItemResponseDto dto = new CartItemResponseDto();
		when(cartMapper.toCartItemResponse(any(CartEntry.class))).thenReturn(dto);

		Model model = new ExtendedModelMap();

		// when
		cartService.showCart(model);

		// then
		verify(cartMapper, times(2)).toCartItemResponse(any(CartEntry.class));
	}

	// ==================== Tests for updateCart() ====================

	@Test
	void shouldUpdateCartWithPlusAction() {
		// given
		CartUpdateRequestDto request = new CartUpdateRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);

		// when
		String redirect = cartService.updateCart(request);

		// then
		assertEquals("redirect:/cart/items", redirect);
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void shouldUpdateCartWithMinusAction() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());

		CartUpdateRequestDto request = new CartUpdateRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.MINUS);

		// when
		String redirect = cartService.updateCart(request);

		// then
		assertEquals("redirect:/cart/items", redirect);
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void shouldUpdateCartWithDeleteAction() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());

		CartUpdateRequestDto request = new CartUpdateRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.DELETE);

		// when
		String redirect = cartService.updateCart(request);

		// then
		assertEquals("redirect:/cart/items", redirect);
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldUpdateCartAndRedirectToCartPage() {
		// given
		CartUpdateRequestDto request = new CartUpdateRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);

		// when
		String redirect = cartService.updateCart(request);

		// then
		assertTrue(redirect.contains("redirect:/cart/items"));
	}

	// ==================== Tests for changeItemCount() ====================

	@Test
	void shouldChangeItemCountWithPlusAction() {
		// given
		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);
		request.setSearch("test");
		request.setSort(SortType.ALPHA);
		request.setPageNumber(2);
		request.setPageSize(10);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		assertTrue(redirect.contains("search=test"));
		assertTrue(redirect.contains("sort=ALPHA"));
		assertTrue(redirect.contains("pageNumber=2"));
		assertTrue(redirect.contains("pageSize=10"));
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
	}

	@Test
	void shouldChangeItemCountWithMinusAction() {
		// given
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());

		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.MINUS);
		request.setSearch("");
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(5);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void shouldChangeItemCountWithNullSearch() {
		// given
		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);
		request.setSearch(null);
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(5);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		assertTrue(redirect.contains("search="));
	}

	@Test
	void shouldChangeItemCountWithNullSort() {
		// given
		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);
		request.setSearch("test");
		request.setSort(null);
		request.setPageNumber(1);
		request.setPageSize(5);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		assertTrue(redirect.contains("sort=NO"));
	}

	@Test
	void shouldChangeItemCountWithNullPageNumber() {
		// given
		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);
		request.setSearch("test");
		request.setSort(SortType.PRICE);
		request.setPageNumber(null);
		request.setPageSize(5);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		assertTrue(redirect.contains("pageNumber=0"));
	}

	@Test
	void shouldChangeItemCountWithNullPageSize() {
		// given
		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);
		request.setSearch("test");
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(null);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		assertTrue(redirect.contains("pageSize=5"));
	}

	@Test
	void shouldChangeItemCountAndUrlEncodeSearch() {
		// given
		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);
		request.setSearch("test search with spaces");
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(5);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		assertTrue(redirect.contains("search=test+search+with+spaces"));
	}

	@Test
	void shouldChangeItemCountWithAllDefaultValues() {
		// given
		ChangeItemCountRequestDto request = new ChangeItemCountRequestDto();
		request.setId(testItem1.getId());
		request.setAction(CartAction.PLUS);
		request.setSearch(null);
		request.setSort(null);
		request.setPageNumber(null);
		request.setPageSize(null);

		// when
		String redirect = cartService.changeItemCount(request);

		// then
		assertTrue(redirect.startsWith("redirect:/items?"));
		assertTrue(redirect.contains("search="));
		assertTrue(redirect.contains("sort=NO"));
		assertTrue(redirect.contains("pageNumber=0"));
		assertTrue(redirect.contains("pageSize=5"));
	}

	// ==================== Tests for changeItemCountOnDetails() ====================

	@Test
	void shouldChangeItemCountOnDetailsWithPlusAction() {
		// given
		Long itemId = testItem1.getId();

		// when
		String redirect = cartService.changeItemCountOnDetails(itemId, CartAction.PLUS);

		// then
		assertEquals("redirect:/items/" + itemId, redirect);
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void shouldChangeItemCountOnDetailsWithMinusAction() {
		// given
		Long itemId = testItem1.getId();
		cartService.addItem(itemId);
		cartService.addItem(itemId);

		// when
		String redirect = cartService.changeItemCountOnDetails(itemId, CartAction.MINUS);

		// then
		assertEquals("redirect:/items/" + itemId, redirect);
		List<CartEntry> items = cartService.getItems();
		assertEquals(1, items.size());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void shouldChangeItemCountOnDetailsWithDeleteAction() {
		// given
		Long itemId = testItem1.getId();
		cartService.addItem(itemId);
		cartService.addItem(itemId);

		// when
		String redirect = cartService.changeItemCountOnDetails(itemId, CartAction.DELETE);

		// then
		assertEquals("redirect:/items/" + itemId, redirect);
		List<CartEntry> items = cartService.getItems();
		assertTrue(items.isEmpty());
	}

	@Test
	void shouldChangeItemCountOnDetailsAndRedirectToCorrectPage() {
		// given
		Long itemId = testItem2.getId();

		// when
		String redirect = cartService.changeItemCountOnDetails(itemId, CartAction.PLUS);

		// then
		assertTrue(redirect.contains("redirect:/items/" + itemId));
	}

	@Test
	void shouldChangeItemCountOnDetailsForDifferentItems() {
		// given
		Long itemId1 = testItem1.getId();
		Long itemId2 = testItem2.getId();

		// when
		String redirect1 = cartService.changeItemCountOnDetails(itemId1, CartAction.PLUS);
		String redirect2 = cartService.changeItemCountOnDetails(itemId2, CartAction.PLUS);

		// then
		assertEquals("redirect:/items/" + itemId1, redirect1);
		assertEquals("redirect:/items/" + itemId2, redirect2);
		List<CartEntry> items = cartService.getItems();
		assertEquals(2, items.size());
	}

	// ==================== Integration Tests ====================

	@Test
	void shouldHandleComplexCartOperations() {
		// Add multiple items
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());
		cartService.addItem(testItem3.getId());
		cartService.addItem(testItem3.getId());
		cartService.addItem(testItem3.getId());

		// Verify state
		assertEquals(3, cartService.getItems().size());
		assertEquals(13000L, cartService.getTotalPrice()); // 2*1000 + 1*2000 + 3*3000 = 2000 + 2000 + 9000

		// Remove one item
		cartService.removeOne(testItem1.getId());
		assertEquals(12000L, cartService.getTotalPrice()); // 1*1000 + 1*2000 + 3*3000 = 1000 + 2000 + 9000

		// Remove all of one type
		cartService.removeAll(testItem3.getId());
		assertEquals(3000L, cartService.getTotalPrice()); // 1*1000 + 1*2000 = 1000 + 2000

		// Clear cart
		cartService.clear();
		assertTrue(cartService.getItems().isEmpty());
		assertEquals(0L, cartService.getTotalPrice());
	}

	@Test
	void shouldMaintainCartStateAcrossMultipleOperations() {
		// Add items
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem1.getId());
		assertEquals(2, cartService.getItems().get(0).getCount());

		// Remove one
		cartService.removeOne(testItem1.getId());
		assertEquals(1, cartService.getItems().get(0).getCount());

		// Add again
		cartService.addItem(testItem1.getId());
		assertEquals(2, cartService.getItems().get(0).getCount());

		// Remove all
		cartService.removeAll(testItem1.getId());
		assertTrue(cartService.getItems().isEmpty());
	}

	@Test
	void shouldHandleNullParametersGracefully() {
		// These should not throw exceptions
		cartService.addItem(null);
		cartService.removeOne(null);
		cartService.removeAll(null);

		// Cart should remain empty
		assertTrue(cartService.getItems().isEmpty());
		assertEquals(0L, cartService.getTotalPrice());
	}

	@Test
	void shouldReturnConsistentResults() {
		// Add items
		cartService.addItem(testItem1.getId());
		cartService.addItem(testItem2.getId());

		// Get items multiple times
		List<CartEntry> items1 = cartService.getItems();
		List<CartEntry> items2 = cartService.getItems();

		// Should return same data
		assertEquals(items1.size(), items2.size());

		// Get total multiple times
		long total1 = cartService.getTotalPrice();
		long total2 = cartService.getTotalPrice();

		// Should return same value
		assertEquals(total1, total2);
	}
}
