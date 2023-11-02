package com.onemsg.javaservice;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JavaServiceApplication {

	public static void main(String[] args) {
		var app = new SpringApplication(JavaServiceApplication.class);
		app.setBannerMode(Mode.OFF);
		app.run(args);
	}
}
