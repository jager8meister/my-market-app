package ru.yandex.practicum.mymarket.service.impl;

import java.util.Comparator;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.model.ItemImageModel;
import ru.yandex.practicum.mymarket.enums.SortType;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;
	private final ItemMapper itemMapper;

	@Override
	public Flux<ItemResponseDto> getItems(ItemsFilterRequestDto request) {
		log.debug("getItems called with request: {}", request);
		String search = request.search();
		boolean hasSearch = search != null && !search.isBlank();
		Flux<ItemEntity> itemsFlux = hasSearch
				? itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search)
				: itemRepository.findAll();

		SortType sort = request.sort() == null ? SortType.NO : request.sort();
		itemsFlux = switch (sort) {
			case ALPHA -> itemsFlux.sort(Comparator.comparing(item -> item.getTitle().toLowerCase()));
			case PRICE -> itemsFlux.sort(Comparator.comparingLong(ItemEntity::getPrice));
			case NO -> itemsFlux;
		};

		int pageNumber = request.pageNumber() == null ? 1 : Math.max(1, request.pageNumber());
		int pageSize = request.pageSize() == null ? 5 : Math.max(1, request.pageSize());
		long offset = (long) (pageNumber - 1) * pageSize;

		return itemsFlux
				.skip(offset)
				.take(pageSize)
				.map(item -> itemMapper.toItemResponse(item, 0));
	}

	@Override
	public Mono<ItemDetailsResponseDto> getItem(Long id) {
		log.debug("getItem called with id: {}", id);
		return itemRepository.findById(id)
				.switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + id)))
				.map(item -> itemMapper.toItemDetailsResponse(item, 0));
	}

	@Override
	public Mono<ResponseEntity<byte[]>> getItemImageResponse(Long id) {
		log.debug("getItemImageResponse called with id: {}", id);
		return itemImageRepository.findByItemId(id)
				.switchIfEmpty(Mono.error(new ItemNotFoundException("Item image not found for item id: " + id)))
				.map(entity -> new ItemImageModel(entity.getData(), entity.getContentType()))
				.map(this::toImageResponse);
	}

	private ResponseEntity<byte[]> toImageResponse(ItemImageModel image) {
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
}
