package ru.yandex.practicum.mymarket.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.yandex.practicum.mymarket.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

	@Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items")
	Page<OrderEntity> findAllWithItems(Pageable pageable);

	@Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
	Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);
}
