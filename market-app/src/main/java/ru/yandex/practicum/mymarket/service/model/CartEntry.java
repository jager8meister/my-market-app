package ru.yandex.practicum.mymarket.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.mymarket.entity.ItemEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartEntry {

	private ItemEntity item;

	private int count;
}
