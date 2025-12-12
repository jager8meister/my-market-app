package ru.yandex.practicum.mymarket.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.mymarket.dto.request.CartUpdateRequestDto;
import ru.yandex.practicum.mymarket.dto.request.ChangeItemCountRequestDto;
import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.mapper.CartMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.model.CartEntry;
import ru.yandex.practicum.mymarket.service.session.SessionCartStorage;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

	private final ItemRepository itemRepository;
	private final CartMapper cartMapper;
	private final SessionCartStorage cartStorage;

	@Override
	@Transactional(readOnly = true)
	public void addItem(Long itemId) {
		log.debug("addItem called with itemId: {}", itemId);
		if (itemId == null) {
			return;
		}
		Map<Long, Integer> itemIdToCount = cartStorage.getCart();
		itemRepository.findById(itemId).ifPresent(item -> {
			int currentCount = itemIdToCount.getOrDefault(itemId, 0);
			itemIdToCount.put(itemId, currentCount + 1);
			log.debug("Item {} count increased to {}", itemId, currentCount + 1);
		});
	}

	@Override
	public void removeOne(Long itemId) {
		log.debug("removeOne called with itemId: {}", itemId);
		if (itemId == null) {
			return;
		}
		Map<Long, Integer> itemIdToCount = cartStorage.getCart();
		Integer current = itemIdToCount.get(itemId);
		if (current == null || current <= 0) {
			return;
		}
		if (current == 1) {
			itemIdToCount.remove(itemId);
		} else {
			itemIdToCount.put(itemId, current - 1);
		}
	}

	@Override
	public void removeAll(Long itemId) {
		log.debug("removeAll called with itemId: {}", itemId);
		if (itemId == null) {
			return;
		}
		cartStorage.getCart().remove(itemId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CartEntry> getItems() {
		Map<Long, Integer> itemIdToCount = cartStorage.getCart();
		if (itemIdToCount.isEmpty()) {
			return new ArrayList<>();
		}

		List<ItemEntity> items = itemRepository.findAllById(itemIdToCount.keySet());

		List<CartEntry> entries = new ArrayList<>();
		for (ItemEntity item : items) {
			Integer count = itemIdToCount.get(item.getId());
			if (count != null && count > 0) {
				entries.add(new CartEntry(item, count));
			}
		}
		return entries;
	}

	@Override
	@Transactional(readOnly = true)
	public long getTotalPrice() {
		return getItems().stream()
				.mapToLong(entry -> entry.getItem().getPrice() * entry.getCount())
				.sum();
	}

	@Override
	public void clear() {
		log.info("clear called - clearing cart");
		cartStorage.clear();
	}

	@Override
	public String showCart(Model model) {
		log.debug("showCart called");
		List<CartItemResponseDto> items = getItems()
				.stream()
				.map(cartMapper::toCartItemResponse)
				.toList();

		long total = getTotalPrice();

		model.addAttribute("items", items);
		model.addAttribute("total", total);

		return "cart";
	}

	@Override
	public String updateCart(CartUpdateRequestDto request) {
		log.debug("updateCart called with request: {}", request);
		applyCartAction(request.getAction(), request.getId());

		return "redirect:/cart/items";
	}

	@Override
	public String changeItemCount(ChangeItemCountRequestDto request) {
		log.debug("changeItemCount called with request: {}", request);
		applyCartAction(request.getAction(), request.getId());

		String encodedSearch = request.getSearch() == null ? "" : URLEncoder.encode(request.getSearch(), StandardCharsets.UTF_8);
		String encodedSort = request.getSort() == null ? "NO" : request.getSort().name();
		int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 0;
		int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;

		return "redirect:/items?search=" + encodedSearch + "&sort=" + encodedSort + "&pageNumber=" + pageNumber + "&pageSize=" + pageSize;
	}

	@Override
	public String changeItemCountOnDetails(Long id, CartAction action) {
		log.debug("changeItemCountOnDetails called with id: {}, action: {}", id, action);
		applyCartAction(action, id);

		return "redirect:/items/" + id;
	}

	private void applyCartAction(CartAction action, Long itemId) {
		switch (action) {
			case PLUS -> addItem(itemId);
			case MINUS -> removeOne(itemId);
			case DELETE -> removeAll(itemId);
		}
	}
}
