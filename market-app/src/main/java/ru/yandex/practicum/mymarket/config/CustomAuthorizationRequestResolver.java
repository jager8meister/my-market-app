package ru.yandex.practicum.mymarket.config;

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Custom authorization request resolver that adds prompt=login parameter
 * to force Keycloak to show the login page even if there's an active SSO session.
 */
public class CustomAuthorizationRequestResolver implements ServerOAuth2AuthorizationRequestResolver {

	private final ServerOAuth2AuthorizationRequestResolver defaultResolver;

	public CustomAuthorizationRequestResolver(
			ReactiveClientRegistrationRepository clientRegistrationRepository) {
		this.defaultResolver = new DefaultServerOAuth2AuthorizationRequestResolver(
				clientRegistrationRepository);
	}

	@Override
	public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
		return defaultResolver.resolve(exchange)
				.map(this::customizeAuthorizationRequest);
	}

	@Override
	public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
		return defaultResolver.resolve(exchange, clientRegistrationId)
				.map(this::customizeAuthorizationRequest);
	}

	private OAuth2AuthorizationRequest customizeAuthorizationRequest(
			OAuth2AuthorizationRequest authorizationRequest) {

		return OAuth2AuthorizationRequest
				.from(authorizationRequest)
				.additionalParameters(params -> {
					params.put("prompt", "login");
					params.put("max_age", "0");
				})
				.build();
	}
}
