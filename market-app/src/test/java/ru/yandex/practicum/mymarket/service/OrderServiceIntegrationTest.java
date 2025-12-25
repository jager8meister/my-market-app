package ru.yandex.practicum.mymarket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.WebSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.client.PaymentClient;
import ru.yandex.practicum.mymarket.dto.response.OrderItemResponseDto;
import ru.yandex.practicum.mymarket.dto.response.OrderResponseDto;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.entity.UserEntity;
import ru.yandex.practicum.mymarket.service.impl.OrderServiceImpl;
import ru.yandex.practicum.mymarket.service.model.CartEntry;
import ru.yandex.practicum.payment.client.model.PaymentResponse;
import ru.yandex.practicum.payment.client.model.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class OrderServiceIntegrationTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private CartService cartService;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private TransactionalOperator transactionalOperator;

	@Mock
	private PaymentClient paymentClient;

	@Mock
	private UserService userService;

	@Mock
	private WebSession session;

	@InjectMocks
	private OrderServiceImpl orderService;

	private ItemEntity testItem;
	private CartEntry cartEntry;
	private OrderEntity savedOrder;
	private OrderItemEntity orderItem;
	private PaymentResponse paymentResponse;

	@BeforeEach
	void setUp() {
		testItem = new ItemEntity(1L, "Test Item", "Description", 1000L, "test.jpg");

		cartEntry = new CartEntry(testItem, 2);

		savedOrder = new OrderEntity(1L, 1L, 2000L, LocalDateTime.now());

		orderItem = new OrderItemEntity(1L, 1L, "Test Item", 1000L, 2);

		paymentResponse = new PaymentResponse();
		paymentResponse.setId(1L);
		paymentResponse.setOrderId(1L);
		paymentResponse.setUserId(1L);
		paymentResponse.setAmount(2000L);
		paymentResponse.setStatus(PaymentStatus.COMPLETED);

		UserEntity testUser = new UserEntity();
		testUser.setId(1L);
		testUser.setUsername("testuser");
		testUser.setBalance(1000000L);

		when(userService.getCurrentUserId()).thenReturn(Mono.just(1L));
		when(userService.getCurrentUser()).thenReturn(Mono.just(testUser));
		when(userService.hasEnoughBalance(anyLong(), anyLong())).thenReturn(Mono.just(true));

		when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
			Mono<?> mono = invocation.getArgument(0);
			return mono;
		});
	}

	@Test
	void buy_shouldCreateOrderAndPayment() {
		OrderItemResponseDto itemDto = new OrderItemResponseDto("Test Item", 1000L, 2);
		OrderResponseDto expectedResponse = new OrderResponseDto(1L, List.of(itemDto), 2000L, LocalDateTime.now());

		when(cartService.getItems(session)).thenReturn(Flux.just(cartEntry));
		when(orderRepository.save(any(OrderEntity.class))).thenReturn(Mono.just(savedOrder));
		when(orderItemRepository.save(any(OrderItemEntity.class))).thenReturn(Mono.just(orderItem));
		when(paymentClient.createPayment(anyLong(), anyLong(), anyLong(), anyString())).thenReturn(Mono.just(paymentResponse));
		when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(orderItem));
		when(orderMapper.toOrderItemResponse(orderItem)).thenReturn(itemDto);
		when(cartService.clear(session)).thenReturn(Mono.empty());

		Mono<OrderResponseDto> result = orderService.buy(session);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertThat(response).isNotNull();
					assertThat(response.id()).isEqualTo(1L);
					assertThat(response.totalSum()).isEqualTo(2000L);
					assertThat(response.items()).hasSize(1);
				})
				.verifyComplete();

		verify(cartService).getItems(session);
		verify(orderRepository).save(any(OrderEntity.class));
		verify(orderItemRepository).save(any(OrderItemEntity.class));
		verify(paymentClient).createPayment(1L, 1L, 2000L, "Оплата заказа #1");
		verify(cartService).clear(session);
	}

	@Test
	void buy_shouldFailWhenCartIsEmpty() {
		when(cartService.getItems(session)).thenReturn(Flux.empty());

		Mono<OrderResponseDto> result = orderService.buy(session);

		StepVerifier.create(result)
				.expectErrorMatches(throwable ->
					throwable.getMessage().contains("Cannot create order from empty cart"))
				.verify();

		verify(cartService).getItems(session);
	}

	@Test
	void buy_shouldFailWhenPaymentFails() {
		when(cartService.getItems(session)).thenReturn(Flux.just(cartEntry));
		when(orderRepository.save(any(OrderEntity.class))).thenReturn(Mono.just(savedOrder));
		when(orderItemRepository.save(any(OrderItemEntity.class))).thenReturn(Mono.just(orderItem));
		when(paymentClient.createPayment(anyLong(), anyLong(), anyLong(), anyString()))
				.thenReturn(Mono.error(new RuntimeException("Payment service unavailable")));

		Mono<OrderResponseDto> result = orderService.buy(session);

		StepVerifier.create(result)
				.expectErrorMatches(throwable ->
					throwable.getMessage().contains("Не удалось создать платеж"))
				.verify();

		verify(paymentClient).createPayment(anyLong(), anyLong(), anyLong(), anyString());
	}
}
