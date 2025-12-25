package ru.yandex.practicum.mymarket.exception;

public class ServiceUnavailableException extends RuntimeException {

	public ServiceUnavailableException(String message) {
		super(message);
	}
}
