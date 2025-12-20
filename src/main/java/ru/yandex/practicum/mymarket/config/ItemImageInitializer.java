package ru.yandex.practicum.mymarket.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
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
	private static final String DEFAULT_IMG_PATH = "images/android_phone.png";

	@EventListener(ApplicationReadyEvent.class)
	public void fillImagesIfMissing() {
		log.info("Starting item image initialization");
		itemRepository.findAll()
				.flatMap(this::ensureImageForItem)
				.onErrorContinue((ex, obj) -> log.error("Failed to init image for {}: {}", obj, ex.getMessage()))
				.subscribe(
						ignored -> {},
						err -> log.error("Image init failed: {}", err.getMessage()),
						() -> log.info("Item image initialization completed successfully")
				);
	}

	private Mono<ItemImageEntity> ensureImageForItem(ItemEntity item) {
		return itemImageRepository.findByItemId(item.getId())
				.switchIfEmpty(loadAndSaveImage(item));
	}

	private Mono<ItemImageEntity> loadAndSaveImage(ItemEntity item) {
		String imgPath = item.getImgPath();
		if (imgPath == null || imgPath.isBlank()) {
			log.warn("Item {} has empty image path, using default {}", item.getId(), DEFAULT_IMG_PATH);
			imgPath = DEFAULT_IMG_PATH;
		}

		String resourcePath = "static/" + imgPath;
		ClassPathResource resource = new ClassPathResource(resourcePath);

		if (!resource.exists()) {
			return Mono.error(new ImageInitializationException("Failed to load image for item " + item.getId() + ": resource " + resourcePath + " not found"));
		}

		try (InputStream inputStream = resource.getInputStream()) {
			byte[] data = inputStream.readAllBytes();
			ItemImageEntity image = new ItemImageEntity();
			image.setItemId(item.getId());
			image.setData(data);
			image.setContentType(resolveContentType(imgPath));
			return itemImageRepository.save(image)
					.doOnSuccess(saved -> log.debug("Loaded image for item id: {}", item.getId()));
		} catch (IOException e) {
			log.error("Failed to load image for item {}: {}", item.getId(), e.getMessage());
			return Mono.error(new ImageInitializationException("Failed to load image for item " + item.getId() + ": " + e.getMessage()));
		}
	}

	private String resolveContentType(String imgPath) {
		String lowerCasePath = imgPath.toLowerCase(Locale.ROOT);
		if (lowerCasePath.endsWith(".png")) {
			return "image/png";
		}
		if (lowerCasePath.endsWith(".jpg") || lowerCasePath.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if (lowerCasePath.endsWith(".svg")) {
			return "image/svg+xml";
		}
		return "application/octet-stream";
	}
}
