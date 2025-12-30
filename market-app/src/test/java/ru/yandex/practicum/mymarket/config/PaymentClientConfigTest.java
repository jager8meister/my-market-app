package ru.yandex.practicum.mymarket.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.payment.client.invoker.ApiClient;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentClientConfigTest {

	@Test
	void shouldCreateAuthorizedClientManager() {
		PaymentClientConfig config = new PaymentClientConfig();
		ReactiveClientRegistrationRepository repository = createTestRepository();

		assertThat(config.authorizedClientManager(repository)).isNotNull();
	}

	@Test
	void shouldCreatePaymentWebClientWithOAuth2Filter() {
		PaymentClientConfig config = new PaymentClientConfig();
		ReactiveClientRegistrationRepository repository = createTestRepository();

		WebClient webClient = config.paymentWebClient(
				WebClient.builder(),
				config.authorizedClientManager(repository));

		assertThat(webClient).isNotNull();
	}

	@Test
	void shouldCreatePaymentApiClient() {
		PaymentClientConfig config = new PaymentClientConfig();
		WebClient webClient = WebClient.builder().build();

		ApiClient apiClient = config.paymentApiClient(webClient);

		assertThat(apiClient).isNotNull();
	}

	private ReactiveClientRegistrationRepository createTestRepository() {
		ClientRegistration registration = ClientRegistration
				.withRegistrationId("market-app")
				.clientId("test-client")
				.clientSecret("test-secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.tokenUri("http://localhost:8180/token")
				.build();

		return new InMemoryReactiveClientRegistrationRepository(registration);
	}
}
