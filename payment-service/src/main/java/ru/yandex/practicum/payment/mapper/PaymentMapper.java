package ru.yandex.practicum.payment.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.yandex.practicum.payment.entity.PaymentEntity;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "failureReason", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	PaymentEntity toEntity(PaymentRequest request);

	PaymentResponse toResponse(PaymentEntity entity);

	default OffsetDateTime map(java.time.LocalDateTime localDateTime) {
		return localDateTime == null ? null : localDateTime.atOffset(ZoneOffset.UTC);
	}
}
