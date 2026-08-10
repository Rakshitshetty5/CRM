package com.flowcrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FlowcrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowcrmApplication.class, args);
	}

}
