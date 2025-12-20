package ru.yandex.practicum.mymarket.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEntity {

	@Id
	private Long id;

	@Column("order_id")
	private Long orderId;

	@Column("title")
	private String title;

	@Column("price")
	private long price;

	@Column("count")
	private int count;

	public OrderItemEntity(String title, long price, int count) {
		this.title = title;
		this.price = price;
		this.count = count;
	}
}
