package ru.yandex.practicum.mymarket.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.practicum.mymarket.controllers.ItemImageController;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebMvcTest(ItemImageController.class)
class ItemImageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	@Test
	void shouldReturnItemImage() throws Exception {
		byte[] imageData = "test image data".getBytes();
		ResponseEntity<byte[]> response = ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
				.body(imageData);

		when(itemService.getItemImageResponse(eq(1L)))
				.thenReturn(response);

		mockMvc.perform(get("/items/1/image"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
				.andExpect(content().bytes(imageData));

		verify(itemService, times(1)).getItemImageResponse(eq(1L));
	}

	@Test
	void shouldReturnImageForDifferentItemId() throws Exception {
		byte[] imageData = "another image".getBytes();
		ResponseEntity<byte[]> response = ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
				.body(imageData);

		when(itemService.getItemImageResponse(eq(5L)))
				.thenReturn(response);

		mockMvc.perform(get("/items/5/image"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/svg+xml"))
				.andExpect(content().bytes(imageData));

		verify(itemService, times(1)).getItemImageResponse(eq(5L));
	}

	@Test
	void shouldReturnNoContentWhenImageNotFound() throws Exception {
		ResponseEntity<byte[]> response = ResponseEntity.noContent().build();

		when(itemService.getItemImageResponse(eq(99L)))
				.thenReturn(response);

		mockMvc.perform(get("/items/99/image"))
				.andExpect(status().isNoContent());

		verify(itemService, times(1)).getItemImageResponse(eq(99L));
	}
}
