package com.inventaria.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InventariaApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventariaApplication.class, args);
	}

}
