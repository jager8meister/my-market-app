package ru.yandex.practicum.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.entity.PaymentEntity;
import ru.yandex.practicum.payment.mapper.PaymentMapper;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.model.PaymentStatus;
import ru.yandex.practicum.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentMapper paymentMapper;

	@InjectMocks
	private PaymentService paymentService;

	private PaymentRequest paymentRequest;
	private PaymentEntity paymentEntity;
	private PaymentResponse paymentResponse;

	@BeforeEach
	void setUp() {
		paymentRequest = new PaymentRequest();
		paymentRequest.setOrderId(1L);
		paymentRequest.setUserId(1L);
		paymentRequest.setAmount(10000L);
		paymentRequest.setDescription("Test payment");

		paymentEntity = new PaymentEntity();
		paymentEntity.setId(1L);
		paymentEntity.setOrderId(1L);
		paymentEntity.setUserId(1L);
		paymentEntity.setAmount(10000L);
		paymentEntity.setDescription("Test payment");
		paymentEntity.setStatus(PaymentStatus.PENDING);

		paymentResponse = new PaymentResponse();
		paymentResponse.setId(1L);
		paymentResponse.setOrderId(1L);
		paymentResponse.setAmount(10000L);
		paymentResponse.setStatus(PaymentStatus.COMPLETED);
	}

	@Test
	void createPayment_shouldCreateAndProcessPayment() {
		when(paymentMapper.toEntity(any(PaymentRequest.class))).thenReturn(paymentEntity);
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
			PaymentEntity entity = invocation.getArgument(0);
			entity.setId(1L);
			return Mono.just(entity);
		});
		when(paymentMapper.toResponse(any(PaymentEntity.class))).thenReturn(paymentResponse);

		Mono<PaymentResponse> result = paymentService.createPayment(paymentRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertThat(response).isNotNull();
					assertThat(response.getId()).isEqualTo(1L);
					assertThat(response.getOrderId()).isEqualTo(1L);
					assertThat(response.getAmount()).isEqualTo(10000L);
				})
				.verifyComplete();

		verify(paymentMapper).toEntity(paymentRequest);
		verify(paymentRepository, times(2)).save(any(PaymentEntity.class)); 
		verify(paymentMapper).toResponse(any(PaymentEntity.class));
	}

	@Test
	void getPayment_shouldReturnPayment() {
		PaymentEntity savedEntity = new PaymentEntity();
		savedEntity.setId(1L);
		savedEntity.setOrderId(1L);
		savedEntity.setAmount(10000L);
		savedEntity.setStatus(PaymentStatus.COMPLETED);

		when(paymentRepository.findById(1L)).thenReturn(Mono.just(savedEntity));
		when(paymentMapper.toResponse(savedEntity)).thenReturn(paymentResponse);

		Mono<PaymentResponse> result = paymentService.getPayment(1L);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertThat(response).isNotNull();
					assertThat(response.getId()).isEqualTo(1L);
					assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
				})
				.verifyComplete();

		verify(paymentRepository).findById(1L);
		verify(paymentMapper).toResponse(savedEntity);
	}

	@Test
	void getPayment_shouldReturnErrorWhenNotFound() {
		when(paymentRepository.findById(999L)).thenReturn(Mono.empty());

		Mono<PaymentResponse> result = paymentService.getPayment(999L);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable.getMessage().contains("Payment not found"))
				.verify();

		verify(paymentRepository).findById(999L);
	}

	@Test
	void cancelPayment_shouldCancelPayment() {
		PaymentEntity existingEntity = new PaymentEntity();
		existingEntity.setId(1L);
		existingEntity.setStatus(PaymentStatus.PENDING);

		PaymentEntity cancelledEntity = new PaymentEntity();
		cancelledEntity.setId(1L);
		cancelledEntity.setStatus(PaymentStatus.CANCELLED);

		PaymentResponse cancelledResponse = new PaymentResponse();
		cancelledResponse.setId(1L);
		cancelledResponse.setStatus(PaymentStatus.CANCELLED);

		when(paymentRepository.findById(1L)).thenReturn(Mono.just(existingEntity));
		when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(Mono.just(cancelledEntity));
		when(paymentMapper.toResponse(cancelledEntity)).thenReturn(cancelledResponse);

		Mono<PaymentResponse> result = paymentService.cancelPayment(1L);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertThat(response).isNotNull();
					assertThat(response.getId()).isEqualTo(1L);
					assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
				})
				.verifyComplete();

		verify(paymentRepository).findById(1L);
		verify(paymentRepository).save(any(PaymentEntity.class));
		verify(paymentMapper).toResponse(cancelledEntity);
	}
}
