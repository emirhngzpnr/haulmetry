package com.truckpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TruckpulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(TruckpulseApplication.class, args);
	}

}
