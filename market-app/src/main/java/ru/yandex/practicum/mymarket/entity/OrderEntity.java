package ru.yandex.practicum.mymarket.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

	@Id
	private Long id;

	@Column("user_id")
	private Long userId;

	@Column("total_sum")
	private Long totalSum;

	@Column("created_at")
	private LocalDateTime createdAt;
}
