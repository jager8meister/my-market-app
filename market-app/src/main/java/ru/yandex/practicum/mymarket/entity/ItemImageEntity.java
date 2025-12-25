package ru.yandex.practicum.mymarket.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("item_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemImageEntity {

	@Id
	private Long id;

	@Column("item_id")
	private Long itemId;

	@Column("data")
	private byte[] data;

	@Column("content_type")
	private String contentType;
}
