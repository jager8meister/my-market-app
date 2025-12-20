package ru.yandex.practicum.mymarket.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemEntity {

	@Id
	private Long id;

	@Column("title")
	private String title;

	@Column("description")
	private String description;

	@Column("price")
	private long price;

	@Column("img_path")
	private String imgPath;
}
