package ru.yandex.practicum.payment.exception;

public class PaymentOperationException extends RuntimeException {

	public PaymentOperationException(String message) {
		super(message);
	}
}
