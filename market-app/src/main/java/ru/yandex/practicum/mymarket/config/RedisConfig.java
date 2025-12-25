package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

	@Bean
	public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
			ReactiveRedisConnectionFactory connectionFactory) {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
				.<String, Object>newSerializationContext(new StringRedisSerializer())
				.value(serializer)
				.hashValue(serializer)
				.build();

		return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
	}
}
