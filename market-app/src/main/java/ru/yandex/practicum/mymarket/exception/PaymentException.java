package ru.yandex.practicum.mymarket.exception;

public class PaymentException extends RuntimeException {

	public PaymentException(String message) {
		super(message);
	}
}
