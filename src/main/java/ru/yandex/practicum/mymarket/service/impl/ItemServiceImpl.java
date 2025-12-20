package ru.yandex.practicum.mymarket.service.impl;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import ru.yandex.practicum.mymarket.enums.SortType;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;
	private final ItemMapper itemMapper;

	@Override
	public Mono<Page<ItemResponseDto>> getItems(ItemsFilterRequestDto filter, Pageable pageable) {
		log.debug("getItems called with filter: {}, pageable: {}", filter, pageable);
		String search = filter.search();
		boolean hasSearch = search != null && !search.isBlank();
		Flux<ItemEntity> itemsFlux = hasSearch
				? itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search)
				: itemRepository.findAll();

		SortType sort = filter.sort() == null ? SortType.NO : filter.sort();
		itemsFlux = switch (sort) {
			case ALPHA -> itemsFlux.sort(Comparator.comparing(item -> item.getTitle().toLowerCase()));
			case PRICE -> itemsFlux.sort(Comparator.comparingLong(ItemEntity::getPrice));
			case NO -> itemsFlux;
		};

		long offset = pageable.getOffset();
		int pageSize = pageable.getPageSize();

		return itemsFlux.count()
				.zipWith(
					itemsFlux
						.skip(offset)
						.take(pageSize)
						.map(item -> itemMapper.toItemResponse(item, 0))
						.collectList()
				)
				.map(tuple -> {
					long total = tuple.getT1();
					var content = tuple.getT2();
					return new PageImpl<>(content, pageable, total);
				});
	}

	@Override
	public Mono<Page<ItemResponseDto>> getItemsWithCartCounts(ItemsFilterRequestDto filter, Pageable pageable, CartStateResponseDto cart) {
		log.debug("getItemsWithCartCounts called with filter: {}, pageable: {}, cart: {}", filter, pageable, cart);
		Map<Long, Integer> cartCountMap = cart.items().stream()
				.collect(Collectors.toMap(CartItemResponseDto::id, CartItemResponseDto::count));

		String search = filter.search();
		boolean hasSearch = search != null && !search.isBlank();
		Flux<ItemEntity> itemsFlux = hasSearch
				? itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search)
				: itemRepository.findAll();

		SortType sort = filter.sort() == null ? SortType.NO : filter.sort();
		itemsFlux = switch (sort) {
			case ALPHA -> itemsFlux.sort(Comparator.comparing(item -> item.getTitle().toLowerCase()));
			case PRICE -> itemsFlux.sort(Comparator.comparingLong(ItemEntity::getPrice));
			case NO -> itemsFlux;
		};

		long offset = pageable.getOffset();
		int pageSize = pageable.getPageSize();

		return itemsFlux.count()
				.zipWith(
					itemsFlux
						.skip(offset)
						.take(pageSize)
						.map(item -> {
							int count = cartCountMap.getOrDefault(item.getId(), 0);
							return itemMapper.toItemResponse(item, count);
						})
						.collectList()
				)
				.map(tuple -> {
					long total = tuple.getT1();
					var content = tuple.getT2();
					return new PageImpl<>(content, pageable, total);
				});
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<ItemDetailsResponseDto> getItem(Long id) {
		log.debug("getItem called with id: {}", id);
		return itemRepository.findById(id)
				.switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + id)))
				.map(item -> itemMapper.toItemDetailsResponse(item, 0));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<ItemDetailsResponseDto> getItemWithCartCount(Long id, int count) {
		log.debug("getItemWithCartCount called with id: {}, count: {}", id, count);
		return itemRepository.findById(id)
				.switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + id)))
				.map(item -> itemMapper.toItemDetailsResponse(item, count));
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<ResponseEntity<byte[]>> getItemImageResponse(Long id) {
		log.debug("getItemImageResponse called with id: {}", id);
		return itemImageRepository.findByItemId(id)
				.switchIfEmpty(Mono.error(new ItemNotFoundException("Item image not found for item id: " + id)))
				.map(this::toImageResponse);
	}

	private ResponseEntity<byte[]> toImageResponse(ItemImageEntity entity) {
		byte[] data = entity.getData();
		if (data == null || data.length == 0) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		String contentType = entity.getContentType();
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (contentType != null && !contentType.isBlank()) {
			mediaType = MediaType.parseMediaType(contentType);
		}
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
				.body(data);
	}
}
