package Seatsure.Backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableScheduling
@EnableResilientMethods
public class SeatsureBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeatsureBackendApplication.class, args);
	}

}
