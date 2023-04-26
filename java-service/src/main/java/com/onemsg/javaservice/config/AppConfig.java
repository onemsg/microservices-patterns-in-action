package com.onemsg.javaservice.config;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

@EnableAsync
@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
public class AppConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(f));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(f));

        DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        module.addSerializer(LocalDate.class, new LocalDateSerializer(f2));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(f2));

        DateTimeFormatter f3 = DateTimeFormatter.ofPattern("HH:mm:ss");
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(f3));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(f3));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

}
