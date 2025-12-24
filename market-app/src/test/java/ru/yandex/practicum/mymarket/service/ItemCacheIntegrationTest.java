package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.dto.request.ItemsFilterRequestDto;
import ru.yandex.practicum.mymarket.dto.response.ItemDetailsResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemImageEntity;
import ru.yandex.practicum.mymarket.enums.SortType;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@SpringBootTest
@Testcontainers
class ItemCacheIntegrationTest extends AbstractIntegrationTest {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", redis::getFirstMappedPort);
	}

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private ItemImageRepository itemImageRepository;

	@Autowired
	private ReactiveRedisTemplate<String, Object> redisTemplate;

	private Long testItemId;

	@BeforeEach
	void setUp() {
		redisTemplate.getConnectionFactory()
				.getReactiveConnection()
				.serverCommands()
				.flushAll()
				.block();

		ItemEntity item = new ItemEntity();
		item.setTitle("Test Item for Cache");
		item.setDescription("Test Description");
		item.setPrice(1000L);
		item.setImgPath("test.png");

		testItemId = itemRepository.save(item)
				.map(ItemEntity::getId)
				.block();

		ItemImageEntity image = new ItemImageEntity();
		image.setItemId(testItemId);
		image.setData(new byte[]{1, 2, 3, 4, 5});
		image.setContentType("image/png");
		itemImageRepository.save(image).block();
	}

	@Test
	void shouldCacheItemDetails() {
		String cacheKey = "item:" + testItemId;

		StepVerifier.create(itemService.getItem(testItemId))
				.assertNext(item -> {
					assert item.id().equals(testItemId);
					assert item.title().equals("Test Item for Cache");
				})
				.verifyComplete();

		StepVerifier.create(
				redisTemplate.hasKey(cacheKey)
		)
				.expectNext(true)
				.verifyComplete();

		StepVerifier.create(itemService.getItem(testItemId))
				.assertNext(item -> {
					assert item.id().equals(testItemId);
					assert item.title().equals("Test Item for Cache");
				})
				.verifyComplete();

		StepVerifier.create(
				redisTemplate.opsForValue().get(cacheKey)
						.cast(ItemDetailsResponseDto.class)
		)
				.assertNext(cached -> {
					assert cached.id().equals(testItemId);
					assert cached.title().equals("Test Item for Cache");
				})
				.verifyComplete();
	}

	@Test
	void shouldCacheItemImage() {
		String cacheKey = "item-image:" + testItemId;

		StepVerifier.create(itemService.getItemImageResponse(testItemId))
				.assertNext(response -> {
					assert response.getStatusCode().is2xxSuccessful();
					assert response.getBody() != null;
					assert response.getBody().length == 5;
				})
				.verifyComplete();

		StepVerifier.create(
				redisTemplate.hasKey(cacheKey)
		)
				.expectNext(true)
				.verifyComplete();

		StepVerifier.create(itemService.getItemImageResponse(testItemId))
				.assertNext(response -> {
					assert response.getStatusCode().is2xxSuccessful();
					assert response.getBody() != null;
					assert response.getBody().length == 5;
				})
				.verifyComplete();
	}

	@Test
	void shouldRespectCacheTTL() throws InterruptedException {
		String cacheKey = "item:" + testItemId;

		itemService.getItem(testItemId).block();

		StepVerifier.create(
				redisTemplate.getExpire(cacheKey)
		)
				.assertNext(ttl -> {
					assert ttl.getSeconds() > 3500 && ttl.getSeconds() <= 3600;
				})
				.verifyComplete();
	}

	@Test
	void shouldHandleCacheMiss() {
		Long nonExistentId = 99999L;
		String cacheKey = "item:" + nonExistentId;

		StepVerifier.create(redisTemplate.hasKey(cacheKey))
				.expectNext(false)
				.verifyComplete();

		StepVerifier.create(itemService.getItem(nonExistentId))
				.expectError()
				.verify();

		StepVerifier.create(redisTemplate.hasKey(cacheKey))
				.expectNext(false)
				.verifyComplete();
	}

	@Test
	void shouldEvictCache() {
		String cacheKey = "item:" + testItemId;

		itemService.getItem(testItemId).block();

		StepVerifier.create(redisTemplate.hasKey(cacheKey))
				.expectNext(true)
				.verifyComplete();

		StepVerifier.create(
				redisTemplate.delete(cacheKey)
		)
				.expectNext(1L)
				.verifyComplete();

		StepVerifier.create(redisTemplate.hasKey(cacheKey))
				.expectNext(false)
				.verifyComplete();
	}

	@Test
	void shouldFallbackToDatabaseOnCacheError() {
		StepVerifier.create(itemService.getItem(testItemId))
				.assertNext(item -> {
					assert item.id().equals(testItemId);
					assert item.title().equals("Test Item for Cache");
				})
				.verifyComplete();
	}

	@Test
	void shouldCacheItemsList() {
		ItemsFilterRequestDto filter = new ItemsFilterRequestDto(null, null);
		Pageable pageable = PageRequest.of(0, 5);
		String cacheKey = "items:list:search=:sort=NO:page=0:size=5";

		StepVerifier.create(itemService.getItems(filter, pageable))
				.assertNext(page -> {
					assert page.getContent().size() > 0;
					assert page.getTotalElements() > 0;
				})
				.verifyComplete();

		StepVerifier.create(redisTemplate.hasKey(cacheKey))
				.expectNext(true)
				.verifyComplete();

		StepVerifier.create(itemService.getItems(filter, pageable))
				.assertNext(page -> {
					assert page.getContent().size() > 0;
					assert page.getTotalElements() > 0;
				})
				.verifyComplete();
	}

	@Test
	void shouldCacheItemsListWithSearch() {
		ItemsFilterRequestDto filter = new ItemsFilterRequestDto("Test", null);
		Pageable pageable = PageRequest.of(0, 5);
		String cacheKey = "items:list:search=Test:sort=NO:page=0:size=5";

		StepVerifier.create(itemService.getItems(filter, pageable))
				.assertNext(page -> {
					assert page.getContent().size() > 0;
				})
				.verifyComplete();

		StepVerifier.create(redisTemplate.hasKey(cacheKey))
				.expectNext(true)
				.verifyComplete();
	}

	@Test
	void shouldCacheItemsListWithSort() {
		ItemsFilterRequestDto filter = new ItemsFilterRequestDto(null, SortType.PRICE);
		Pageable pageable = PageRequest.of(0, 5);
		String cacheKey = "items:list:search=:sort=PRICE:page=0:size=5";

		StepVerifier.create(itemService.getItems(filter, pageable))
				.assertNext(page -> {
					assert page.getContent().size() > 0;
				})
				.verifyComplete();

		StepVerifier.create(redisTemplate.hasKey(cacheKey))
				.expectNext(true)
				.verifyComplete();
	}

	@Test
	void shouldRespectItemsListCacheTTL() {
		ItemsFilterRequestDto filter = new ItemsFilterRequestDto(null, null);
		Pageable pageable = PageRequest.of(0, 5);
		String cacheKey = "items:list:search=:sort=NO:page=0:size=5";

		itemService.getItems(filter, pageable).block();

		StepVerifier.create(redisTemplate.getExpire(cacheKey))
				.assertNext(ttl -> {
					assert ttl.getSeconds() > 250 && ttl.getSeconds() <= 300;
				})
				.verifyComplete();
	}

	@Test
	void shouldCacheDifferentPagesIndependently() {
		ItemsFilterRequestDto filter = new ItemsFilterRequestDto(null, null);
		Pageable page1 = PageRequest.of(0, 5);
		Pageable page2 = PageRequest.of(1, 5);
		String cacheKey1 = "items:list:search=:sort=NO:page=0:size=5";
		String cacheKey2 = "items:list:search=:sort=NO:page=1:size=5";

		itemService.getItems(filter, page1).block();
		itemService.getItems(filter, page2).block();

		StepVerifier.create(redisTemplate.hasKey(cacheKey1))
				.expectNext(true)
				.verifyComplete();

		StepVerifier.create(redisTemplate.hasKey(cacheKey2))
				.expectNext(true)
				.verifyComplete();
	}
}
