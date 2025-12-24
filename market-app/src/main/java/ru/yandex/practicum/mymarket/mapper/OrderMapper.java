package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;

import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;

@Mapper(componentModel = "spring")
public interface OrderMapper {

	OrderItemResponseDto toOrderItemResponse(OrderItemEntity item);
}
