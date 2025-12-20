package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;

import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.service.model.OrderItemModel;
import ru.yandex.practicum.mymarket.service.model.OrderModel;

@Mapper(componentModel = "spring")
public interface OrderMapper {

	OrderItemResponseDto toOrderItemResponse(OrderItemModel item);

	OrderResponseDto toOrderResponse(OrderModel order);
}
