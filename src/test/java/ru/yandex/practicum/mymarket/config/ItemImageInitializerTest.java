package ru.yandex.practicum.mymarket.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemImageEntity;
import ru.yandex.practicum.mymarket.exception.ImageInitializationException;
import ru.yandex.practicum.mymarket.repository.ItemImageRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@ExtendWith(MockitoExtension.class)
class ItemImageInitializerTest {

	@Mock
	private ItemRepository itemRepository;

	@Mock
	private ItemImageRepository itemImageRepository;

	@Mock
	private ApplicationReadyEvent applicationReadyEvent;

	@InjectMocks
	private ItemImageInitializer itemImageInitializer;

	private ItemEntity item1;
	private ItemEntity item2;
	private ItemEntity item3;

	@BeforeEach
	void setUp() {
		item1 = new ItemEntity();
		item1.setId(1L);
		item1.setTitle("Item 1");
		item1.setPrice(1000L);
		item1.setImgPath("images/android_phone.png");

		item2 = new ItemEntity();
		item2.setId(2L);
		item2.setTitle("Item 2");
		item2.setPrice(2000L);
		item2.setImgPath("images/iphone.png");

		item3 = new ItemEntity();
		item3.setId(3L);
		item3.setTitle("Item 3");
		item3.setPrice(3000L);
		item3.setImgPath("images/laptop_light.png");
	}

	@Test
	void shouldInitializeImagesForAllItems() {
		// given
		List<ItemEntity> items = Arrays.asList(item1, item2, item3);
		when(itemRepository.findAll()).thenReturn(items);
		when(itemImageRepository.findByItemId(anyLong())).thenReturn(Optional.empty());

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		verify(itemImageRepository, times(3)).save(any(ItemImageEntity.class));
		verify(itemImageRepository).findByItemId(1L);
		verify(itemImageRepository).findByItemId(2L);
		verify(itemImageRepository).findByItemId(3L);
	}

	@Test
	void shouldSkipItemsThatAlreadyHaveImages() {
		// given
		List<ItemEntity> items = Arrays.asList(item1, item2, item3);
		ItemImageEntity existingImage = new ItemImageEntity();
		existingImage.setItem(item1);
		existingImage.setData("existing".getBytes());
		existingImage.setContentType("image/svg+xml");

		when(itemRepository.findAll()).thenReturn(items);
		when(itemImageRepository.findByItemId(1L)).thenReturn(Optional.of(existingImage));
		when(itemImageRepository.findByItemId(2L)).thenReturn(Optional.empty());
		when(itemImageRepository.findByItemId(3L)).thenReturn(Optional.empty());

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		verify(itemImageRepository, times(2)).save(any(ItemImageEntity.class));
		verify(itemImageRepository).findByItemId(1L);
		verify(itemImageRepository).findByItemId(2L);
		verify(itemImageRepository).findByItemId(3L);
	}

	@Test
	void shouldSkipInitializationWhenNoItems() {
		// given
		when(itemRepository.findAll()).thenReturn(new ArrayList<>());

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		verify(itemImageRepository, never()).save(any(ItemImageEntity.class));
		verify(itemImageRepository, never()).findByItemId(anyLong());
	}

	@Test
	void shouldHandleEmptyItemsList() {
		// given
		when(itemRepository.findAll()).thenReturn(List.of());

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		verify(itemRepository).findAll();
		verify(itemImageRepository, never()).findByItemId(anyLong());
		verify(itemImageRepository, never()).save(any(ItemImageEntity.class));
	}

	@Test
	void shouldSaveImagesForMultipleItems() {
		// given
		ItemEntity item4 = new ItemEntity();
		item4.setId(4L);
		item4.setTitle("Item 4");
		item4.setPrice(4000L);
		item4.setImgPath("images/laptop_white.png");
		item4.setImgPath("images/laptop_dark.png");

		ItemEntity item5 = new ItemEntity();
		item5.setId(5L);
		item5.setTitle("Item 5");
		item5.setPrice(5000L);
		item5.setImgPath("images/headphones.png");

		List<ItemEntity> items = Arrays.asList(item1, item2, item3, item4, item5);
		when(itemRepository.findAll()).thenReturn(items);
		when(itemImageRepository.findByItemId(anyLong())).thenReturn(Optional.empty());

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		// Should save image for all 5 items
		verify(itemImageRepository, times(5)).save(any(ItemImageEntity.class));
	}

	@Test
	void shouldHandleSingleItem() {
		// given
		List<ItemEntity> items = List.of(item1);
		when(itemRepository.findAll()).thenReturn(items);
		when(itemImageRepository.findByItemId(1L)).thenReturn(Optional.empty());

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		verify(itemImageRepository, times(1)).save(any(ItemImageEntity.class));
		verify(itemImageRepository).findByItemId(1L);
	}

	@Test
	void shouldHandleAllItemsWithExistingImages() {
		// given
		List<ItemEntity> items = Arrays.asList(item1, item2, item3);
		ItemImageEntity existingImage1 = new ItemImageEntity();
		existingImage1.setItem(item1);
		ItemImageEntity existingImage2 = new ItemImageEntity();
		existingImage2.setItem(item2);
		ItemImageEntity existingImage3 = new ItemImageEntity();
		existingImage3.setItem(item3);

		when(itemRepository.findAll()).thenReturn(items);
		when(itemImageRepository.findByItemId(1L)).thenReturn(Optional.of(existingImage1));
		when(itemImageRepository.findByItemId(2L)).thenReturn(Optional.of(existingImage2));
		when(itemImageRepository.findByItemId(3L)).thenReturn(Optional.of(existingImage3));

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		verify(itemImageRepository, never()).save(any(ItemImageEntity.class));
		verify(itemImageRepository).findByItemId(1L);
		verify(itemImageRepository).findByItemId(2L);
		verify(itemImageRepository).findByItemId(3L);
	}

	@Test
	void shouldHandleMixOfItemsWithAndWithoutImages() {
		// given
		ItemEntity item4 = new ItemEntity();
		item4.setId(4L);
		item4.setTitle("Item 4");
		item4.setPrice(4000L);

		List<ItemEntity> items = Arrays.asList(item1, item2, item3, item4);
		ItemImageEntity existingImage1 = new ItemImageEntity();
		existingImage1.setItem(item1);
		ItemImageEntity existingImage3 = new ItemImageEntity();
		existingImage3.setItem(item3);

		when(itemRepository.findAll()).thenReturn(items);
		when(itemImageRepository.findByItemId(1L)).thenReturn(Optional.of(existingImage1));
		when(itemImageRepository.findByItemId(2L)).thenReturn(Optional.empty());
		when(itemImageRepository.findByItemId(3L)).thenReturn(Optional.of(existingImage3));
		when(itemImageRepository.findByItemId(4L)).thenReturn(Optional.empty());

		// when
		itemImageInitializer.fillImagesIfMissing();

		// then
		verify(itemImageRepository, times(2)).save(any(ItemImageEntity.class));
		verify(itemImageRepository).findByItemId(1L);
		verify(itemImageRepository).findByItemId(2L);
		verify(itemImageRepository).findByItemId(3L);
		verify(itemImageRepository).findByItemId(4L);
	}
}
