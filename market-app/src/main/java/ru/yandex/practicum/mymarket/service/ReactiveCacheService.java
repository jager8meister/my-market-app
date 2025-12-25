package ru.yandex.practicum.mymarket.service;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveCacheService {

	private final ReactiveRedisTemplate<String, Object> redisTemplate;

	public <T> Mono<T> getOrPut(String key, Class<T> valueClass, Mono<T> dataSupplier, Duration ttl) {
		log.debug("Cache lookup for key: {}", key);

		return redisTemplate.opsForValue()
				.get(key)
				.cast(valueClass)
				.doOnNext(cached -> log.debug("Cache HIT for key: {}", key))
				.switchIfEmpty(
						dataSupplier
								.flatMap(data -> {
									log.debug("Cache MISS for key: {}, caching with TTL: {}", key, ttl);
									return redisTemplate.opsForValue()
											.set(key, data, ttl)
											.thenReturn(data);
								})
				)
				.onErrorResume(error -> {
					log.warn("Cache error for key {}: {}, falling back to data supplier",
							key, error.getMessage());
					return dataSupplier;
				});
	}

	public Mono<Boolean> evict(String key) {
		log.debug("Evicting cache key: {}", key);
		return redisTemplate.delete(key)
				.map(count -> count > 0)
				.doOnNext(deleted -> {
					if (deleted) {
						log.debug("Cache evicted for key: {}", key);
					}
				});
	}

	public Mono<Long> evictByPattern(String pattern) {
		log.debug("Evicting cache keys by pattern: {}", pattern);
		return redisTemplate.keys(pattern)
				.flatMap(redisTemplate::delete)
				.reduce(0L, Long::sum)
				.doOnNext(count -> log.info("Evicted {} cache keys for pattern: {}", count, pattern));
	}
}
