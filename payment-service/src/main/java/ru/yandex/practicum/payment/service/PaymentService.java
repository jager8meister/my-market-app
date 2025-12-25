package ru.yandex.practicum.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.entity.PaymentEntity;
import ru.yandex.practicum.payment.exception.PaymentNotFoundException;
import ru.yandex.practicum.payment.exception.PaymentOperationException;
import ru.yandex.practicum.payment.mapper.PaymentMapper;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.model.PaymentStatus;
import ru.yandex.practicum.payment.repository.PaymentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentMapper paymentMapper;
	private final BalanceService balanceService;

	public Mono<PaymentResponse> createPayment(PaymentRequest request) {
		log.info("createPayment called with orderId: {}, amount: {}", request.getOrderId(), request.getAmount());

		return Mono.fromCallable(() -> initializePaymentEntity(request))
				.flatMap(paymentRepository::save)
				.doOnNext(saved -> log.info("Payment entity saved with id: {}", saved.getId()))
				.flatMap(this::processPayment)
				.map(paymentMapper::toResponse)
				.doOnSuccess(response -> log.info("createPayment completed successfully: paymentId={}, status={}",
						response.getId(), response.getStatus()))
				.doOnError(error -> log.error("Failed to create payment for order {}: {}",
						request.getOrderId(), error.getMessage()));
	}

	private PaymentEntity initializePaymentEntity(PaymentRequest request) {
		log.debug("Initializing payment entity for order {}", request.getOrderId());
		PaymentEntity entity = paymentMapper.toEntity(request);
		LocalDateTime now = LocalDateTime.now();
		entity.setStatus(PaymentStatus.PENDING);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		return entity;
	}

	@Transactional(readOnly = true)
	public Mono<PaymentResponse> getPayment(Long id) {
		log.debug("getPayment called with id: {}", id);

		return paymentRepository.findById(id)
				.switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found with id: " + id)))
				.doOnError(error -> log.warn("Payment not found with id: {}", id))
				.map(paymentMapper::toResponse)
				.doOnSuccess(payment -> log.debug("getPayment returned payment: id={}, status={}, amount={}",
						payment.getId(), payment.getStatus(), payment.getAmount()));
	}

	public Mono<PaymentResponse> cancelPayment(Long id) {
		log.info("cancelPayment called with id: {}", id);

		return paymentRepository.findById(id)
				.switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found with id: " + id)))
				.flatMap(payment -> {
					log.debug("Found payment {} with status {}", id, payment.getStatus());

					if (payment.getStatus() != PaymentStatus.PENDING) {
						log.warn("Cannot cancel payment {} - current status: {}", id, payment.getStatus());
						return Mono.error(new PaymentOperationException(
								"Payment cannot be cancelled. Current status: " + payment.getStatus()));
					}

					payment.setStatus(PaymentStatus.CANCELLED);
					payment.setUpdatedAt(LocalDateTime.now());
					return paymentRepository.save(payment);
				})
				.doOnNext(cancelled -> log.info("Payment {} successfully cancelled", id))
				.map(paymentMapper::toResponse)
				.doOnError(error -> {
					if (!(error instanceof PaymentNotFoundException)) {
						log.error("Failed to cancel payment {}: {}", id, error.getMessage());
					}
				});
	}

	private Mono<PaymentEntity> processPayment(PaymentEntity payment) {
		log.debug("processPayment called for payment id: {}", payment.getId());

		return Mono.just(payment)
				.flatMap(p -> {
					log.debug("Processing payment {} for user {}, amount: {}", p.getId(), p.getUserId(), p.getAmount());
					return balanceService.deductBalance(p.getUserId(), p.getAmount())
							.thenReturn(p);
				})
				.flatMap(p -> {
					p.setStatus(PaymentStatus.COMPLETED);
					p.setUpdatedAt(LocalDateTime.now());
					log.info("Payment {} completed successfully for user {}", p.getId(), p.getUserId());
					return paymentRepository.save(p);
				})
				.onErrorResume(error -> {
					log.error("Payment {} failed: {}", payment.getId(), error.getMessage());
					payment.setStatus(PaymentStatus.FAILED);
					payment.setFailureReason(error.getMessage());
					payment.setUpdatedAt(LocalDateTime.now());
					return paymentRepository.save(payment)
							.flatMap(saved -> Mono.error(error));
				})
				.doOnSuccess(processed -> log.debug("Payment {} processing finished with status: {}",
						processed.getId(), processed.getStatus()));
	}
}
