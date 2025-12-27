package ru.yandex.practicum.mymarket.exception;

import java.net.URI;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(-2)
public class WebViewExceptionHandler implements WebExceptionHandler {

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		log.debug("WebViewExceptionHandler handling exception: {} for path: {}",
			ex.getClass().getSimpleName(), exchange.getRequest().getPath());

		if (ex instanceof UserNotFoundException) {
			boolean isHtml = isHtmlRequest(exchange);
			log.warn("User not found - isHtmlRequest: {}, Accept headers: {}, Path: {}",
				isHtml, exchange.getRequest().getHeaders().getAccept(), exchange.getRequest().getPath());

			if (isHtml) {
				log.warn("Redirecting to login due to UserNotFoundException");
				exchange.getResponse().setStatusCode(HttpStatus.FOUND);
				exchange.getResponse().getHeaders().setLocation(URI.create("/login?error=session_expired"));
				return exchange.getResponse().setComplete();
			}
			log.debug("Not HTML request, letting GlobalExceptionHandler handle it");
		}

		return Mono.error(ex);
	}

	private boolean isHtmlRequest(ServerWebExchange exchange) {
		boolean hasHtmlAccept = exchange.getRequest().getHeaders().getAccept().stream()
				.anyMatch(mediaType -> mediaType.includes(MediaType.TEXT_HTML));

		boolean isViewPath = !exchange.getRequest().getPath().value().startsWith("/api/");

		return hasHtmlAccept || isViewPath;
	}
}
