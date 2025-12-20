package ru.yandex.practicum.mymarket.service.model;

import java.util.List;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderModel {

	private long id;
	private List<OrderItemModel> items;
	private long totalSum;
	private LocalDateTime createdAt;
}
