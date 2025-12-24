package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import ru.yandex.practicum.payment.client.invoker.ApiClient;

@Configuration
public class PaymentClientConfig {

	@Value("${payment.service.url:http://localhost:8081}")
	private String paymentServiceUrl;

	@Bean
	public WebClient paymentWebClient(WebClient.Builder builder) {
		return builder
				.baseUrl(paymentServiceUrl)
				.build();
	}

	@Bean
	public ApiClient paymentApiClient(WebClient paymentWebClient) {
		ApiClient apiClient = new ApiClient(paymentWebClient);
		apiClient.setBasePath(paymentServiceUrl);
		return apiClient;
	}
}
