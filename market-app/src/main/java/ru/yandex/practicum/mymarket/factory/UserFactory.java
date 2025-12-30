package ru.yandex.practicum.mymarket.factory;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.mymarket.entity.UserEntity;

@Component
@Slf4j
public class UserFactory {

	public UserEntity createNewOAuth2User(String username) {
		log.debug("Creating new OAuth2 user with username: {}", username);
		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setPassword("");
		return user;
	}
}
