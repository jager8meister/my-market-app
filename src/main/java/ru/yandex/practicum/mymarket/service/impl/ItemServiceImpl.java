package ru.yandex.practicum.mymarket.service.impl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.PagingDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;
import ru.yandex.practicum.mymarket.service.model.ItemImageModel;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

	private static final int ITEMS_PER_ROW = 3;
	private static final long EMPTY_ITEM_MARKER_ID = -1L;
	private static final ItemResponseDto EMPTY_ITEM = new ItemResponseDto(EMPTY_ITEM_MARKER_ID, "", "", "", 0, 0);

	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;
	private final CartService cartService;
	private final ItemMapper itemMapper;

	@Override
	@Transactional(readOnly = true)
	public Page<ItemEntity> getItems(String search, Pageable pageable) {
		log.debug("getItems called with search: {}, pageable: {}", search, pageable);
		String trimmedSearch = search == null ? "" : search.trim();
		boolean hasSearch = !trimmedSearch.isEmpty();

		if (hasSearch) {
			return itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
					trimmedSearch, trimmedSearch, pageable);
		}
		return itemRepository.findAll(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ItemEntity> getItem(Long id) {
		log.debug("getItem called with id: {}", id);
		if (id == null) {
			return Optional.empty();
		}
		return itemRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ItemImageModel> getItemImage(Long id) {
		log.debug("getItemImage called with id: {}", id);
		if (id == null) {
			return Optional.empty();
		}
		return itemImageRepository.findByItemId(id)
				.map(entity -> new ItemImageModel(entity.getData(), entity.getContentType()));
	}

	@Override
	public String showItems(ItemsFilterRequestDto request, Model model) {
		log.debug("showItems called with request: {}", request);

		Sort sortSpec = switch (request.getSort()) {
			case ALPHA -> Sort.by("title").ascending();
			case PRICE -> Sort.by("price").ascending();
			case NO -> Sort.unsorted();
		};

		// Convert 1-based pageNumber to 0-based for Spring Data
		int zeroBasedPageNumber = Math.max(0, request.getPageNumber() - 1);

		Pageable pageable = PageRequest.of(
				zeroBasedPageNumber,
				request.getPageSize(),
				sortSpec
		);

		Page<ItemEntity> page = getItems(request.getSearch(), pageable);

		Map<Long, Integer> counts = getCartItemCounts();

		List<ItemResponseDto> flatItems = page.getContent()
				.stream()
				.map(entity -> itemMapper.toItemResponse(entity, counts.getOrDefault(entity.getId(), 0)))
				.toList();

		List<List<ItemResponseDto>> itemsByRows = toRows(flatItems, ITEMS_PER_ROW);

		// Convert 0-based page number back to 1-based
		int oneBasedPageNumber = page.getNumber() + 1;

		PagingDto paging = new PagingDto(
				page.getSize(),
				oneBasedPageNumber,
				page.hasPrevious(),
				page.hasNext()
		);

		String trimmedSearch = request.getSearch() == null ? "" : request.getSearch().trim();

		model.addAttribute("search", trimmedSearch);
		model.addAttribute("sort", request.getSort());
		model.addAttribute("paging", paging);
		model.addAttribute("items", itemsByRows);

		return "items";
	}

	@Override
	public String showItemDetails(Long id, Model model) {
		log.debug("showItemDetails called with id: {}", id);
		ItemEntity item = getItem(id)
				.orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));

		Map<Long, Integer> counts = getCartItemCounts();
		int count = counts.getOrDefault(item.getId(), 0);

		ItemDetailsResponseDto view = itemMapper.toItemDetailsResponse(item, count);
		model.addAttribute("item", view);

		return "item";
	}

	@Override
	public ResponseEntity<byte[]> getItemImageResponse(Long id) {
		log.debug("getItemImageResponse called with id: {}", id);
		ItemImageModel image = getItemImage(id)
				.orElseThrow(() -> new ItemNotFoundException("Item image not found for item id: " + id));
		byte[] data = image.getData();
		if (data == null || data.length == 0) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}

		String contentType = image.getContentType();
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (contentType != null && !contentType.isBlank()) {
			mediaType = MediaType.parseMediaType(contentType);
		}

		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
				.body(data);
	}

	private Map<Long, Integer> getCartItemCounts() {
		Map<Long, Integer> counts = new HashMap<>();
		cartService.getItems().forEach(entry -> {
			ItemEntity item = entry.getItem();
			if (item != null && item.getId() != null) {
				counts.put(item.getId(), entry.getCount());
			}
		});
		return counts;
	}

	private List<List<ItemResponseDto>> toRows(List<ItemResponseDto> items, int itemsPerRow) {
		List<List<ItemResponseDto>> rows = new ArrayList<>();
		if (itemsPerRow <= 0) {
			rows.add(items);
			return rows;
		}
		for (int index = 0; index < items.size(); index += itemsPerRow) {
			int endExclusive = Math.min(index + itemsPerRow, items.size());
			List<ItemResponseDto> row = new ArrayList<>(items.subList(index, endExclusive));
			while (row.size() < itemsPerRow) {
				row.add(EMPTY_ITEM);
			}
			rows.add(row);
		}
		return rows;
	}
}
