package ru.yandex.practicum.mymarket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.practicum.mymarket.entity.ItemImageEntity;

public interface ItemImageRepository extends JpaRepository<ItemImageEntity, Long> {

	Optional<ItemImageEntity> findByItemId(Long itemId);
}
