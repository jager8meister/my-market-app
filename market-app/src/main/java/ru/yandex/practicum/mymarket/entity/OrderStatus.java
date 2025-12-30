package ru.yandex.practicum.mymarket.entity;

/**
 * Order status enum for tracking order lifecycle
 */
public enum OrderStatus {
	/**
	 * Order created, payment pending
	 */
	PENDING,

	/**
	 * Payment successful, order paid
	 */
	PAID,

	/**
	 * Payment failed, order cancelled
	 */
	FAILED
}
