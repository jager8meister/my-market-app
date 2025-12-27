package ru.yandex.practicum.mymarket.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Custom logout success handler that invalidates the WebSession before delegating to OIDC logout.
 * This ensures that session-based data (like the shopping cart) is cleared when the user logs out.
 */
@Slf4j
public class CustomLogoutSuccessHandler implements ServerLogoutSuccessHandler {

	private final OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutHandler;

	public CustomLogoutSuccessHandler(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		this.oidcLogoutHandler = new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
		this.oidcLogoutHandler.setPostLogoutRedirectUri("{baseUrl}/login?logout");
	}

	@Override
	public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
		log.debug("CustomLogoutSuccessHandler: invalidating WebSession before OIDC logout");

		return exchange.getExchange().getSession()
			.flatMap(session -> {
				log.debug("Invalidating session: {}", session.getId());
				return session.invalidate()
					.then(Mono.defer(() -> {
						log.debug("Session invalidated, proceeding with OIDC logout");
						return oidcLogoutHandler.onLogoutSuccess(exchange, authentication);
					}));
			});
	}
}
