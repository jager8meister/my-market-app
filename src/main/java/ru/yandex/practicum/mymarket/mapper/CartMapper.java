package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.yandex.practicum.mymarket.dto.response.CartItemResponseDto;
import ru.yandex.practicum.mymarket.service.model.CartEntry;

@Mapper(componentModel = "spring")
public interface CartMapper {

	@Mapping(target = "id", source = "item.id")
	@Mapping(target = "title", source = "item.title")
	@Mapping(target = "description", source = "item.description")
	@Mapping(target = "price", source = "item.price")
	@Mapping(target = "imgPath", source = "item.imgPath")
	@Mapping(target = "count", source = "count")
	CartItemResponseDto toCartItemResponse(CartEntry entry);
}
