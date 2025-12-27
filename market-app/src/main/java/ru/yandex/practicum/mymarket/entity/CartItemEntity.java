package ru.yandex.practicum.mymarket.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemEntity {

	@Id
	private Long id;

	@Column("user_id")
	private Long userId;

	@Column("item_id")
	private Long itemId;

	@Column("count")
	private Integer count;

	@Column("created_at")
	private LocalDateTime createdAt;

	@Column("updated_at")
	private LocalDateTime updatedAt;
}
