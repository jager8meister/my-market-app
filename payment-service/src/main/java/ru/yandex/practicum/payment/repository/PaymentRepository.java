package ru.yandex.practicum.payment.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import ru.yandex.practicum.payment.entity.PaymentEntity;

@Repository
public interface PaymentRepository extends ReactiveCrudRepository<PaymentEntity, Long> {

}
