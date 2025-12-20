package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.entity.ItemEntity;

@DataR2dbcTest
@Testcontainers
class ItemRepositoryIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("testdb")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () -> String.format(
				"r2dbc:postgresql://%s:%d/%s",
				postgres.getHost(),
				postgres.getFirstMappedPort(),
				postgres.getDatabaseName()
		));
		registry.add("spring.r2dbc.username", postgres::getUsername);
		registry.add("spring.r2dbc.password", postgres::getPassword);
	}

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private DatabaseClient databaseClient;

	@BeforeEach
	void setUp() {
		databaseClient.sql("""
				CREATE TABLE IF NOT EXISTS items (
					id SERIAL PRIMARY KEY,
					title VARCHAR(255) NOT NULL,
					description TEXT,
					price BIGINT NOT NULL,
					img_path VARCHAR(255)
				)
				""")
				.fetch()
				.rowsUpdated()
				.block();

		itemRepository.deleteAll().block();
	}

	@Test
	void shouldSaveAndFindItem() {
		ItemEntity item = new ItemEntity(null, "Test Item", "Test Description", 1000L, "test.png");

		StepVerifier.create(itemRepository.save(item))
				.expectNextMatches(saved -> saved.getId() != null)
				.verifyComplete();
	}

	@Test
	void shouldFindByTitleContaining() {
		ItemEntity item1 = new ItemEntity(null, "Test Phone", "Description", 1000L, "phone.png");
		ItemEntity item2 = new ItemEntity(null, "Test Laptop", "Description", 2000L, "laptop.png");

		itemRepository.save(item1).block();
		itemRepository.save(item2).block();

		StepVerifier.create(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("phone", "phone"))
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void shouldFindAllItems() {
		ItemEntity item1 = new ItemEntity(null, "Item 1", "Desc 1", 100L, "img1.png");
		ItemEntity item2 = new ItemEntity(null, "Item 2", "Desc 2", 200L, "img2.png");

		itemRepository.save(item1).block();
		itemRepository.save(item2).block();

		StepVerifier.create(itemRepository.findAll())
				.expectNextCount(2)
				.verifyComplete();
	}
}
