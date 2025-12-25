package ru.yandex.practicum.mymarket.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.mymarket.security.ReactiveUserDetailsServiceImpl;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final ReactiveUserDetailsServiceImpl userDetailsService;

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
			ReactiveAuthenticationManager authenticationManager) {
		RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
		logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/login?logout"));

		return http
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
						.anyExchange().authenticated())
				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.authenticationManager(authenticationManager)
						.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/items")))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessHandler(logoutSuccessHandler))
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.build();
	}

	@Bean
	public ReactiveAuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
		UserDetailsRepositoryReactiveAuthenticationManager manager = new UserDetailsRepositoryReactiveAuthenticationManager(
				userDetailsService);
		manager.setPasswordEncoder(passwordEncoder);
		return manager;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
