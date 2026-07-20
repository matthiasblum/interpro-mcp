package uk.ac.ebi.interpro.mcp.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * One {@link RestClient} bean per backend service. Each is qualified by its bean
 * name and injected into the matching client wrapper.
 */
@Configuration
public class RestClientConfig {

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }

    @Bean
    public RestClient interProRestClient(@Value("${interpro.api.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()).build();
    }

    @Bean
    public RestClient ebiSearchRestClient(@Value("${ebisearch.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()).build();
    }

    @Bean
    public RestClient matchesRestClient(@Value("${interpro.matches.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()).build();
    }

    @Bean
    public RestClient interProScanRestClient(@Value("${interproscan.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()).build();
    }
}
