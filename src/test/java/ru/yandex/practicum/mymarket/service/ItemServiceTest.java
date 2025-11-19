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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.PagingDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemImageEntity;
import ru.yandex.practicum.mymarket.enums.SortType;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.model.CartEntry;
import ru.yandex.practicum.mymarket.service.model.ItemImageModel;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceTest {

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private ItemImageRepository itemImageRepository;

	@MockBean
	private CartService cartService;

	private ItemEntity testItem;
	private ItemEntity testItem2;
	private ItemEntity testItem3;

	@BeforeEach
	void setUp() {
		testItem = new ItemEntity();
		testItem.setTitle("Смартфон TestPhone");
		testItem.setDescription("Отличный смартфон для тестирования");
		testItem.setPrice(25000L);
		testItem = itemRepository.save(testItem);

		testItem2 = new ItemEntity();
		testItem2.setTitle("Ноутбук Laptop");
		testItem2.setDescription("Мощный ноутбук");
		testItem2.setPrice(50000L);
		testItem2 = itemRepository.save(testItem2);

		testItem3 = new ItemEntity();
		testItem3.setTitle("Наушники Headphones");
		testItem3.setDescription("Качественные наушники");
		testItem3.setPrice(5000L);
		testItem3 = itemRepository.save(testItem3);

		// Setup mock for CartService
		when(cartService.getItems()).thenReturn(new ArrayList<>());
	}

	@Test
	void shouldGetItemById() {
		// when
		Optional<ItemEntity> foundItem = itemService.getItem(testItem.getId());

		// then
		assertTrue(foundItem.isPresent());
		assertEquals(testItem.getId(), foundItem.get().getId());
		assertEquals("Смартфон TestPhone", foundItem.get().getTitle());
	}

	@Test
	void shouldReturnEmptyWhenItemNotFound() {
		// when
		Optional<ItemEntity> item = itemService.getItem(999999L);

		// then
		assertTrue(item.isEmpty());
	}

	@Test
	void shouldReturnEmptyWhenItemIdIsNull() {
		// when
		Optional<ItemEntity> item = itemService.getItem(null);

		// then
		assertTrue(item.isEmpty());
	}

	@Test
	void shouldGetItemsWithPagination() {
		// when
		Page<ItemEntity> items = itemService.getItems("", PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertTrue(items.getTotalElements() > 0);
	}

	@Test
	void shouldSearchItemsByTitle() {
		// when
		Page<ItemEntity> items = itemService.getItems("TestPhone", PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertTrue(items.getTotalElements() > 0);
		assertTrue(items.getContent().stream()
				.anyMatch(item -> item.getTitle().contains("TestPhone")));
	}

	@Test
	void shouldSearchItemsByDescription() {
		// when
		Page<ItemEntity> items = itemService.getItems("тестирования", PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertTrue(items.getTotalElements() > 0);
		assertTrue(items.getContent().stream()
				.anyMatch(item -> item.getDescription().contains("тестирования")));
	}

	@Test
	void shouldSearchCaseInsensitive() {
		// when
		Page<ItemEntity> items = itemService.getItems("testphone", PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertTrue(items.getTotalElements() > 0);
	}

	@Test
	void shouldReturnEmptyPageWhenSearchNotFound() {
		// when
		Page<ItemEntity> items = itemService.getItems("НесуществующийТовар12345", PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertEquals(0, items.getTotalElements());
	}

	@Test
	void shouldHandleNullSearch() {
		// when
		Page<ItemEntity> items = itemService.getItems(null, PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertTrue(items.getTotalElements() > 0);
	}

	@Test
	void shouldHandleEmptySearch() {
		// when
		Page<ItemEntity> items = itemService.getItems("", PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertTrue(items.getTotalElements() > 0);
	}

	@Test
	void shouldHandleWhitespaceSearch() {
		// when
		Page<ItemEntity> items = itemService.getItems("   ", PageRequest.of(0, 10));

		// then
		assertNotNull(items);
		assertTrue(items.getTotalElements() > 0);
	}

	// Tests for showItems method
	@Test
	void shouldShowItemsWithNoSort() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(6);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		assertNotNull(model.getAttribute("items"));
		assertNotNull(model.getAttribute("paging"));
		assertEquals(SortType.NO, model.getAttribute("sort"));
		assertEquals("", model.getAttribute("search"));
	}

	@Test
	void shouldShowItemsWithAlphaSort() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.ALPHA);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		assertEquals(SortType.ALPHA, model.getAttribute("sort"));
		@SuppressWarnings("unchecked")
		List<List<ItemResponseDto>> items = (List<List<ItemResponseDto>>) model.getAttribute("items");
		assertNotNull(items);
	}

	@Test
	void shouldShowItemsWithPriceSort() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.PRICE);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		assertEquals(SortType.PRICE, model.getAttribute("sort"));
	}

	@Test
	void shouldShowItemsWithSearch() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch("TestPhone");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		assertEquals("TestPhone", model.getAttribute("search"));
		@SuppressWarnings("unchecked")
		List<List<ItemResponseDto>> items = (List<List<ItemResponseDto>>) model.getAttribute("items");
		assertNotNull(items);
	}

	@Test
	void shouldShowItemsWithCartCounts() {
		// given
		List<CartEntry> cartItems = new ArrayList<>();
		CartEntry entry = new CartEntry(testItem, 2);
		cartItems.add(entry);
		when(cartService.getItems()).thenReturn(cartItems);

		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		@SuppressWarnings("unchecked")
		List<List<ItemResponseDto>> items = (List<List<ItemResponseDto>>) model.getAttribute("items");
		assertNotNull(items);
	}

	@Test
	void shouldShowItemsWithPagination() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(2);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		PagingDto paging = (PagingDto) model.getAttribute("paging");
		assertNotNull(paging);
		assertEquals(2, paging.getPageSize());
		assertEquals(1, paging.getPageNumber());
		assertTrue(paging.isHasNext());
		assertFalse(paging.isHasPrevious());
	}

	@Test
	void shouldShowItemsWithSecondPage() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(2);
		request.setPageSize(2);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		PagingDto paging = (PagingDto) model.getAttribute("paging");
		assertNotNull(paging);
		assertEquals(2, paging.getPageNumber());
		assertTrue(paging.isHasPrevious());
	}

	@Test
	void shouldShowItemsWithNullSearch() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch(null);
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		assertEquals("", model.getAttribute("search"));
	}

	@Test
	void shouldShowItemsWithWhitespaceSearch() {
		// given
		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch("   ");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
		assertEquals("", model.getAttribute("search"));
	}

	@Test
	void shouldShowItemsWithCartItemsHavingNullItem() {
		// given
		List<CartEntry> cartItems = new ArrayList<>();
		CartEntry entry1 = new CartEntry(null, 1);
		CartEntry entry2 = new CartEntry(testItem, 2);
		cartItems.add(entry1);
		cartItems.add(entry2);
		when(cartService.getItems()).thenReturn(cartItems);

		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
	}

	@Test
	void shouldShowItemsWithCartItemsHavingNullItemId() {
		// given
		ItemEntity itemWithNullId = new ItemEntity();
		itemWithNullId.setTitle("Test");
		itemWithNullId.setDescription("Test");
		itemWithNullId.setPrice(1000L);
		// Note: not saved to DB, so id is null

		List<CartEntry> cartItems = new ArrayList<>();
		CartEntry entry = new CartEntry(itemWithNullId, 1);
		cartItems.add(entry);
		when(cartService.getItems()).thenReturn(cartItems);

		ItemsFilterRequestDto request = new ItemsFilterRequestDto();
		request.setSort(SortType.NO);
		request.setPageNumber(1);
		request.setPageSize(10);
		request.setSearch("");
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItems(request, model);

		// then
		assertEquals("items", view);
	}

	// Tests for showItemDetails method
	@Test
	void shouldShowItemDetails() {
		// given
		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItemDetails(testItem.getId(), model);

		// then
		assertEquals("item", view);
		ItemDetailsResponseDto item = (ItemDetailsResponseDto) model.getAttribute("item");
		assertNotNull(item);
		assertEquals(testItem.getId(), item.getId());
		assertEquals(testItem.getTitle(), item.getTitle());
		assertEquals(0, item.getCount());
	}

	@Test
	void shouldShowItemDetailsWithCartCount() {
		// given
		List<CartEntry> cartItems = new ArrayList<>();
		cartItems.add(new CartEntry(testItem, 3));
		when(cartService.getItems()).thenReturn(cartItems);

		Model model = new ExtendedModelMap();

		// when
		String view = itemService.showItemDetails(testItem.getId(), model);

		// then
		assertEquals("item", view);
		ItemDetailsResponseDto item = (ItemDetailsResponseDto) model.getAttribute("item");
		assertNotNull(item);
		assertEquals(3, item.getCount());
	}

	@Test
	void shouldThrowExceptionWhenShowItemDetailsForNonExistentItem() {
		// when & then
		assertThrows(ItemNotFoundException.class, () -> {
			itemService.showItemDetails(999999L, new ExtendedModelMap());
		});
	}

	// Tests for getItemImage method
	@Test
	void shouldGetItemImage() {
		// given
		ItemImageEntity imageEntity = new ItemImageEntity();
		imageEntity.setItem(testItem);
		imageEntity.setData("test image data".getBytes());
		imageEntity.setContentType("image/png");
		itemImageRepository.save(imageEntity);

		// when
		Optional<ItemImageModel> image = itemService.getItemImage(testItem.getId());

		// then
		assertTrue(image.isPresent());
		assertArrayEquals("test image data".getBytes(), image.get().getData());
		assertEquals("image/png", image.get().getContentType());
	}

	@Test
	void shouldReturnEmptyWhenImageNotFound() {
		// when
		Optional<ItemImageModel> image = itemService.getItemImage(testItem.getId());

		// then
		assertTrue(image.isEmpty());
	}

	@Test
	void shouldReturnEmptyWhenImageIdIsNull() {
		// when
		Optional<ItemImageModel> image = itemService.getItemImage(null);

		// then
		assertTrue(image.isEmpty());
	}

	// Tests for getItemImageResponse method
	@Test
	void shouldGetItemImageResponse() {
		// given
		ItemImageEntity imageEntity = new ItemImageEntity();
		imageEntity.setItem(testItem);
		imageEntity.setData("test image data".getBytes());
		imageEntity.setContentType("image/svg+xml");
		itemImageRepository.save(imageEntity);

		// when
		ResponseEntity<byte[]> response = itemService.getItemImageResponse(testItem.getId());

		// then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertArrayEquals("test image data".getBytes(), response.getBody());
		assertEquals("image/svg+xml", response.getHeaders().getContentType().toString());
	}

	@Test
	void shouldReturnNoContentWhenImageDataIsEmpty() {
		// given
		ItemImageEntity imageEntity = new ItemImageEntity();
		imageEntity.setItem(testItem);
		imageEntity.setData(new byte[0]);
		imageEntity.setContentType("image/png");
		itemImageRepository.save(imageEntity);

		// when
		ResponseEntity<byte[]> response = itemService.getItemImageResponse(testItem.getId());

		// then
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	void shouldThrowExceptionWhenImageNotFoundForResponse() {
		// when & then
		assertThrows(ItemNotFoundException.class, () -> {
			itemService.getItemImageResponse(testItem.getId());
		});
	}

	@Test
	void shouldGetItemImageResponseWithBlankContentType() {
		// given
		ItemImageEntity imageEntity = new ItemImageEntity();
		imageEntity.setItem(testItem);
		imageEntity.setData("test image data".getBytes());
		imageEntity.setContentType("   ");
		itemImageRepository.save(imageEntity);

		// when
		ResponseEntity<byte[]> response = itemService.getItemImageResponse(testItem.getId());

		// then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(MediaType.APPLICATION_OCTET_STREAM.toString(),
			response.getHeaders().getContentType().toString());
	}

	@Test
	void shouldGetItemImageResponseWithValidContentType() {
		// given
		ItemImageEntity imageEntity = new ItemImageEntity();
		imageEntity.setItem(testItem);
		imageEntity.setData("test image data".getBytes());
		imageEntity.setContentType("image/jpeg");
		itemImageRepository.save(imageEntity);

		// when
		ResponseEntity<byte[]> response = itemService.getItemImageResponse(testItem.getId());

		// then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
	}
}
