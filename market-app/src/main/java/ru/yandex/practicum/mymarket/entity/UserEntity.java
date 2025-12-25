package ru.yandex.practicum.mymarket.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

	@Id
	private Long id;

	@Column("username")
	private String username;

	@Column("password")
	private String password;

	@Column("balance")
	private Long balance;

	@Column("created_at")
	private LocalDateTime createdAt;
}
