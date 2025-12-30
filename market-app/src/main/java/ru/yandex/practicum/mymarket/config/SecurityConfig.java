package ru.yandex.practicum.mymarket.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(
			ServerHttpSecurity http,
			ReactiveClientRegistrationRepository clientRegistrationRepository) {

		CustomLogoutSuccessHandler customLogoutHandler =
			new CustomLogoutSuccessHandler(clientRegistrationRepository);

		CustomAuthorizationRequestResolver authorizationRequestResolver =
			new CustomAuthorizationRequestResolver(clientRegistrationRepository);

		return http
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/login/**", "/oauth2/**", "/logout").permitAll()
						.pathMatchers("/", "/items", "/items/**", "/api/items", "/api/items/**").permitAll()
						.anyExchange().authenticated())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint((exchange, ex) -> {
							exchange.getResponse().setStatusCode(HttpStatus.FOUND);
							exchange.getResponse().getHeaders().setLocation(URI.create("/login"));
							return exchange.getResponse().setComplete();
						}))
				.oauth2Login(oauth2 -> oauth2
						.authorizationRequestResolver(authorizationRequestResolver)
						.authorizationRedirectStrategy(new org.springframework.security.web.server.DefaultServerRedirectStrategy()))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessHandler(customLogoutHandler))
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.build();
	}
}
