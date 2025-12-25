package ru.yandex.practicum.mymarket.service.impl;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CachedItemsPageDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.CartStateResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemImageEntity;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.ReactiveCacheService;
import ru.yandex.practicum.mymarket.enums.SortType;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;
	private final ItemMapper itemMapper;
	private final ReactiveCacheService cacheService;

	@Value("${cache.items.ttl}")
	private Duration itemsCacheTtl;

	private static final String ITEM_CACHE_KEY_PREFIX = "item:";
	private static final String ITEM_IMAGE_CACHE_KEY_PREFIX = "item-image:";
	private static final String ITEMS_LIST_CACHE_KEY_PREFIX = "items:list:";

	@Override
	@Transactional(readOnly = true)
	public Mono<Page<ItemResponseDto>> getItems(ItemsFilterRequestDto filter, Pageable pageable) {
		log.debug("getItems called with filter: {}, pageable: {}", filter, pageable);

		String cacheKey = buildItemsListCacheKey(filter, pageable);

		Mono<CachedItemsPageDto> dataSupplier = getItemsPage(filter, pageable, item -> itemMapper.toItemResponse(item, 0))
				.map(page -> new CachedItemsPageDto(
						page.getContent(),
						page.getNumber(),
						page.getSize(),
						page.getTotalElements()
				))
				.doOnSuccess(cached -> log.debug("getItems loaded from DB: {} items", cached.content().size()));

		return cacheService.getOrPut(cacheKey, CachedItemsPageDto.class, dataSupplier, itemsCacheTtl)
				.<Page<ItemResponseDto>>map(cached -> new PageImpl<>(
						cached.content(),
						PageRequest.of(cached.pageNumber(), cached.pageSize()),
						cached.totalElements()
				))
				.doOnSuccess(page -> log.debug("getItems returned {} items, total: {}",
						page.getNumberOfElements(), page.getTotalElements()));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<Page<ItemResponseDto>> getItemsWithCartCounts(ItemsFilterRequestDto filter, Pageable pageable, CartStateResponseDto cart) {
		log.debug("getItemsWithCartCounts called with filter: {}, pageable: {}, cart items count: {}",
				filter, pageable, cart.items().size());

		Map<Long, Integer> cartCountMap = cart.items().stream()
				.collect(Collectors.toMap(CartItemResponseDto::id, CartItemResponseDto::count));

		return getItems(filter, pageable)
				.<Page<ItemResponseDto>>map(page -> {
					List<ItemResponseDto> itemsWithCartCounts = page.getContent().stream()
							.map(item -> {
								int countInCart = cartCountMap.getOrDefault(item.id(), 0);
								if (item.count() != countInCart) {
									return new ItemResponseDto(
											item.id(),
											item.title(),
											item.description(),
											item.imgPath(),
											item.price(),
											countInCart
									);
								}
								return item;
							})
							.toList();
					return new PageImpl<>(itemsWithCartCounts, pageable, page.getTotalElements());
				})
				.doOnSuccess(page -> log.debug("getItemsWithCartCounts returned {} items (from cache)",
						page.getNumberOfElements()));
	}

	private Mono<Page<ItemResponseDto>> getItemsPage(
			ItemsFilterRequestDto filter,
			Pageable pageable,
			Function<ItemEntity, ItemResponseDto> mapper) {

		return getFilteredAndSortedItems(filter)
				.collectList()
				.map(allItems -> createPage(allItems, pageable, mapper));
	}

	private Flux<ItemEntity> getFilteredAndSortedItems(ItemsFilterRequestDto filter) {
		String search = filter.search();
		Flux<ItemEntity> itemsFlux = (search != null && !search.isBlank())
				? itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search)
				: itemRepository.findAll();

		return applySorting(itemsFlux, filter.sort());
	}

	private Flux<ItemEntity> applySorting(Flux<ItemEntity> itemsFlux, SortType sortType) {
		SortType sort = sortType != null ? sortType : SortType.NO;
		return switch (sort) {
			case ALPHA -> itemsFlux.sort(Comparator.comparing(item -> item.getTitle().toLowerCase()));
			case PRICE -> itemsFlux.sort(Comparator.comparingLong(ItemEntity::getPrice));
			case NO -> itemsFlux;
		};
	}

	private Page<ItemResponseDto> createPage(
			List<ItemEntity> allItems,
			Pageable pageable,
			Function<ItemEntity, ItemResponseDto> mapper) {

		long total = allItems.size();
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), allItems.size());

		List<ItemResponseDto> pageContent = allItems.subList(start, end).stream()
				.map(mapper)
				.toList();

		return new PageImpl<>(pageContent, pageable, total);
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<ItemDetailsResponseDto> getItem(Long id) {
		log.debug("getItem called with id: {}", id);
		String cacheKey = ITEM_CACHE_KEY_PREFIX + id;

		Mono<ItemDetailsResponseDto> dataSupplier = findItemById(id)
				.map(item -> itemMapper.toItemDetailsResponse(item, 0))
				.doOnSuccess(item -> log.debug("getItem loaded from DB: {}", item.title()));

		return cacheService.getOrPut(cacheKey, ItemDetailsResponseDto.class, dataSupplier, itemsCacheTtl)
				.doOnSuccess(item -> log.debug("getItem returned item: {}", item.title()));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<ItemDetailsResponseDto> getItemWithCartCount(Long id, int count) {
		log.debug("getItemWithCartCount called with id: {}, count: {}", id, count);
		return getItem(id)
				.map(cached -> new ItemDetailsResponseDto(
						cached.id(),
						cached.title(),
						cached.description(),
						cached.imgPath(),
						cached.price(),
						count
				))
				.doOnSuccess(item -> log.debug("getItemWithCartCount returned item: {} with count: {}",
						item.title(), count));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<ResponseEntity<byte[]>> getItemImageResponse(Long id) {
		log.debug("getItemImageResponse called with id: {}", id);
		String cacheKey = ITEM_IMAGE_CACHE_KEY_PREFIX + id;

		Mono<ItemImageEntity> dataSupplier = itemImageRepository.findByItemId(id)
				.switchIfEmpty(Mono.error(new ItemNotFoundException("Item image not found for item id: " + id)))
				.doOnSuccess(image -> log.debug("getItemImageResponse loaded from DB for item id: {}, size: {} bytes",
						id, image.getData() != null ? image.getData().length : 0));

		return cacheService.getOrPut(cacheKey, ItemImageEntity.class, dataSupplier, itemsCacheTtl)
				.map(this::toImageResponse)
				.doOnSuccess(response -> log.debug("getItemImageResponse returned image for item id: {}, size: {} bytes",
						id, response.getBody() != null ? response.getBody().length : 0));
	}

	private Mono<ItemEntity> findItemById(Long id) {
		return itemRepository.findById(id)
				.switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + id)))
				.doOnError(error -> log.warn("Item not found with id: {}", id));
	}

	private ResponseEntity<byte[]> toImageResponse(ItemImageEntity entity) {
		byte[] data = entity.getData();
		if (data == null || data.length == 0) {
			log.warn("Item image {} has no data", entity.getItemId());
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}

		MediaType mediaType = parseMediaType(entity.getContentType());
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
				.body(data);
	}

	private MediaType parseMediaType(String contentType) {
		if (contentType != null && !contentType.isBlank()) {
			return MediaType.parseMediaType(contentType);
		}
		return MediaType.APPLICATION_OCTET_STREAM;
	}

	private String buildItemsListCacheKey(ItemsFilterRequestDto filter, Pageable pageable) {
		String search = filter.search() != null ? filter.search() : "";
		String sort = filter.sort() != null ? filter.sort().name() : "NO";
		return String.format("%ssearch=%s:sort=%s:page=%d:size=%d",
				ITEMS_LIST_CACHE_KEY_PREFIX,
				search,
				sort,
				pageable.getPageNumber(),
				pageable.getPageSize()
		);
	}
}
