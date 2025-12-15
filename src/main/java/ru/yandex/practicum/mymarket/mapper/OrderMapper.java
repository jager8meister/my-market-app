package ru.yandex.practicum.mymarket.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.service.model.OrderItemModel;
import ru.yandex.practicum.mymarket.service.model.OrderModel;

@Mapper(componentModel = "spring")
public interface OrderMapper {

	OrderItemResponseDto toOrderItemResponse(OrderItemModel item);

	List<OrderItemResponseDto> toOrderItemResponseList(List<OrderItemModel> items);

	OrderResponseDto toOrderResponse(OrderModel order);

	@Mapping(target = "items", expression = "java(mapOrderItems(entity))")
	OrderModel toOrderModel(OrderEntity entity);

	default List<OrderItemModel> mapOrderItems(OrderEntity entity) {
		return entity.getItems().stream()
				.map(item -> new OrderItemModel(item.getTitle(), item.getPrice(), item.getCount()))
				.toList();
	}
}
