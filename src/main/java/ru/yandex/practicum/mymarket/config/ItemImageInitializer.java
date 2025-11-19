package ru.yandex.practicum.mymarket.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemImageEntity;
import ru.yandex.practicum.mymarket.exception.ImageInitializationException;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ItemImageInitializer {

	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;

	@EventListener(ApplicationReadyEvent.class)
	public void fillImagesIfMissing() {
		log.info("Starting item image initialization");
		List<ItemEntity> items = itemRepository.findAll();

		if (items.isEmpty()) {
			log.warn("No items found in database, skipping image initialization");
			return;
		}

		ClassPathResource[] resources = new ClassPathResource[]{
				new ClassPathResource("static/images/phone.svg"),
				new ClassPathResource("static/images/laptop.svg"),
				new ClassPathResource("static/images/headphones.svg")
		};

		for (int index = 0; index < items.size(); index++) {
			ItemEntity item = items.get(index);

			if (itemImageRepository.findByItemId(item.getId()).isPresent()) {
				continue;
			}

			ClassPathResource resource = resources[index % resources.length];

			try (InputStream inputStream = resource.getInputStream()) {
				byte[] data = inputStream.readAllBytes();
				ItemImageEntity image = new ItemImageEntity();
				image.setItem(item);
				image.setData(data);
				image.setContentType("image/svg+xml");
				itemImageRepository.save(image);
				log.debug("Loaded image for item id: {}", item.getId());
			} catch (IOException e) {
				log.error("Failed to load image for item {}: {}", item.getId(), e.getMessage());
				throw new ImageInitializationException("Failed to load image for item " + item.getId() + ": " + e.getMessage());
			}
		}
		log.info("Item image initialization completed successfully");
	}
}
