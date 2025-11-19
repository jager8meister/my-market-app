package ru.yandex.practicum.mymarket.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemModel {

	private String title;
	private long price;
	private int count;
}

