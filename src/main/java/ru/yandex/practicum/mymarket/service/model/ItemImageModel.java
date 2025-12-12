package ru.yandex.practicum.mymarket.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemImageModel {

	private byte[] data;
	private String contentType;
}

