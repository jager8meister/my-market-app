package ru.yandex.practicum.mymarket.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import reactor.test.StepVerifier;

@SpringBootTest
class RedisManualIntegrationTest {

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", () -> "localhost");
		registry.add("spring.data.redis.port", () -> 6380);

		registry.add("spring.r2dbc.url", () -> "r2dbc:h2:mem:///testdb");
		registry.add("spring.r2dbc.username", () -> "sa");
		registry.add("spring.r2dbc.password", () -> "");
	}

	@Autowired
	private ReactiveRedisTemplate<String, String> redisTemplate;

	@Test
	void shouldConnectToRedis() {
		String key = "manual:test";
		String value = "works!";

		StepVerifier.create(
				redisTemplate.opsForValue().set(key, value)
						.then(redisTemplate.opsForValue().get(key))
		)
				.expectNext(value)
				.verifyComplete();
	}

	@Test
	void shouldIncrementValue() {
		String key = "manual:counter";

		StepVerifier.create(
				redisTemplate.delete(key)
						.then(redisTemplate.opsForValue().increment(key))
						.then(redisTemplate.opsForValue().increment(key))
		)
				.expectNext(2L)
				.verifyComplete();
	}
}
