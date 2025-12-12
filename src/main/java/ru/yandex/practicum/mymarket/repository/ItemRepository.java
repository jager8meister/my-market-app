package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.practicum.mymarket.entity.ItemEntity;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {

	Page<ItemEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
			String title, String description, Pageable pageable);
}
