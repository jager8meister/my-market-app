package ru.yandex.practicum.mymarket.redis;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import reactor.test.StepVerifier;

@SpringBootTest
@Testcontainers
class RedisIntegrationTest {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", redis::getFirstMappedPort);
	}

	@Autowired
	private ReactiveRedisTemplate<String, String> redisTemplate;

	@Test
	void shouldSaveAndRetrieveValue() {
		String key = "test:key";
		String value = "test-value";

		StepVerifier.create(
				redisTemplate.opsForValue().set(key, value)
						.then(redisTemplate.opsForValue().get(key))
		)
				.expectNext(value)
				.verifyComplete();
	}

	@Test
	void shouldSetValueWithExpiration() {
		String key = "test:expiring";
		String value = "will-expire";
		Duration ttl = Duration.ofSeconds(10);

		StepVerifier.create(
				redisTemplate.opsForValue().set(key, value, ttl)
						.then(redisTemplate.getExpire(key))
		)
				.expectNextMatches(duration -> duration.getSeconds() > 0 && duration.getSeconds() <= 10)
				.verifyComplete();
	}

	@Test
	void shouldDeleteValue() {
		String key = "test:delete";
		String value = "to-be-deleted";

		StepVerifier.create(
				redisTemplate.opsForValue().set(key, value)
						.then(redisTemplate.delete(key))
						.then(redisTemplate.opsForValue().get(key))
		)
				.verifyComplete();
	}

	@Test
	void shouldIncrementCounter() {
		String key = "test:counter";

		StepVerifier.create(
				redisTemplate.opsForValue().increment(key)
						.flatMap(count -> redisTemplate.opsForValue().increment(key))
						.flatMap(count -> redisTemplate.opsForValue().increment(key))
		)
				.expectNext(3L)
				.verifyComplete();
	}

	@Test
	void shouldHandleNonExistentKey() {
		String key = "test:nonexistent";

		StepVerifier.create(redisTemplate.opsForValue().get(key))
				.verifyComplete();
	}
}
