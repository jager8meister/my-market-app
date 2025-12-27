package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import ru.yandex.practicum.payment.client.invoker.ApiClient;

@Configuration
public class PaymentClientConfig {

	@Value("${payment.service.url:http://localhost:8081}")
	private String paymentServiceUrl;

	@Bean
	public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository) {

		ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
				ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
						.clientCredentials()
						.build();

		InMemoryReactiveOAuth2AuthorizedClientService clientService =
				new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);

		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
				new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
						clientRegistrationRepository, clientService);

		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		return authorizedClientManager;
	}

	@Bean
	public WebClient paymentWebClient(
			WebClient.Builder builder,
			ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {

		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
				new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

		oauth2.setDefaultClientRegistrationId("market-app");

		return builder
				.filter(oauth2)
				.build();
	}

	@Bean
	public ApiClient paymentApiClient(WebClient paymentWebClient) {
		ApiClient apiClient = new ApiClient(paymentWebClient);
		apiClient.setBasePath(paymentServiceUrl);
		return apiClient;
	}
}
