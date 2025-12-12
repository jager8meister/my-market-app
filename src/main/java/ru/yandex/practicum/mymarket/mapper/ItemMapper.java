package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.dto.response.ItemResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;

@Mapper(componentModel = "spring")
public interface ItemMapper {

	ItemResponseDto toItemResponse(ItemEntity entity, int count);

	ItemDetailsResponseDto toItemDetailsResponse(ItemEntity entity, int count);
}
