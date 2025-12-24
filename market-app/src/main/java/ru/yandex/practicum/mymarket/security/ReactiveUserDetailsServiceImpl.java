package ru.yandex.practicum.mymarket.security;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {

	private final UserRepository userRepository;

	@Override
	public Mono<UserDetails> findByUsername(String username) {
		log.debug("Loading user by username: {}", username);

		return userRepository.findByUsername(username)
				.switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
				.map(user -> User.builder()
						.username(user.getUsername())
						.password(user.getPassword())
						.roles("USER")
						.build())
				.doOnSuccess(user -> log.debug("User loaded successfully: {}", username))
				.doOnError(error -> log.warn("Failed to load user: {}", username));
	}
}
