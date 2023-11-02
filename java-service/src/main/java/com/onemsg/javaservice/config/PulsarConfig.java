package com.onemsg.javaservice.config;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.Setter;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("spring.pulsar.client")
@Setter
@ConditionalOnProperty(name = "app.use.pulsar", havingValue = "true")
public class PulsarConfig {

    private String serviceUrl;

    @Bean(destroyMethod = "close")
    public PulsarClient pulsarClient() throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .build();
    }


    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor pulsarHandleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setDaemon(false);
        executor.setThreadNamePrefix("pulsar-handle-worker-");
        return executor;
    }

}
