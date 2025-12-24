package ru.yandex.practicum.payment.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.payment.model.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payments")
public class PaymentEntity {

	@Id
	private Long id;

	@Column("order_id")
	private Long orderId;

	@Column("user_id")
	private Long userId;

	@Column("amount")
	private Long amount;

	@Column("status")
	private PaymentStatus status;

	@Column("description")
	private String description;

	@Column("failure_reason")
	private String failureReason;

	@CreatedDate
	@Column("created_at")
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column("updated_at")
	private LocalDateTime updatedAt;
}
