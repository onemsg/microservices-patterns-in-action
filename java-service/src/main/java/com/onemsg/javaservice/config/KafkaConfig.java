package com.onemsg.javaservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import  org.springframework.kafka.config.TopicBuilder;

import com.onemsg.commonservice.mq.Topics;

@Configuration
public class KafkaConfig {
    
    @Bean
    public NewTopic asyncJobTopic() {
        return TopicBuilder.name(Topics.ASYNC_JOB_TOPIC)
            .partitions(1)
            .replicas(1)
            .build();
    }
}
