package com.onemsg.javaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

@SpringBootApplication
public class JavaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaServiceApplication.class, args);
	}
}
