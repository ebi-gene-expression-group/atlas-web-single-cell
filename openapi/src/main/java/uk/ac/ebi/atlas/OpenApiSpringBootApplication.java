package uk.ac.ebi.atlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "uk.ac.ebi.atlas")
public class OpenApiSpringBootApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpenApiSpringBootApplication.class, args);
	}
}
