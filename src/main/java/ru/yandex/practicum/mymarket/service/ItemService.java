package ru.yandex.practicum.mymarket.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.enums.CartAction;
import ru.yandex.practicum.mymarket.service.model.ItemImageModel;

public interface ItemService {

	Page<ItemEntity> getItems(String search, Pageable pageable);

	Optional<ItemEntity> getItem(Long id);

	Optional<ItemImageModel> getItemImage(Long id);

	String showItems(ItemsFilterRequestDto request, Model model);

	String showItemDetails(Long id, Model model);

	ResponseEntity<byte[]> getItemImageResponse(Long id);
}
