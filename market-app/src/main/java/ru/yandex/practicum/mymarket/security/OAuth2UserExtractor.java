package ru.yandex.practicum.mymarket.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAuth2UserExtractor {

	public String extractUsername(Authentication auth) {
		if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
			String username = oauth2User.getAttribute("preferred_username");
			if (username == null) {
				username = oauth2User.getAttribute("name");
			}
			if (username == null) {
				username = auth.getName();
			}
			log.debug("Extracted username from OAuth2User: {}", username);
			return username;
		}
		return auth.getName();
	}
}
