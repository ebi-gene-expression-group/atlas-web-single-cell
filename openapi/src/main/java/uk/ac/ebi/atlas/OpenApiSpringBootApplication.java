package uk.ac.ebi.atlas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "uk.ac.ebi.atlas")
@OpenAPIDefinition(
		info = @Info(
				title = "Single Cell Expression Atlas",
				version = "1.0.0",
				description = "Specification of [Single Cell Expression Atlas](https://www.ebi.ac.uk/gxa/sc) API. " +
						"This documents describe a series of endpoints which are usually consumed by front end " +
						"components for navigation and visualisation of datasets and their associated resources.",
				license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html"),
				contact = @Contact(
						url = "https://www.ebi.ac.uk/support/gxasc",
						name = "EBI support & feedback",
						email = "arrayexpress-atlas@ebi.ac.uk")
		),
//		Maybe we could add here some tags to describe that the endpoint is being consumed by a React component.
//		If the above is a good idea we can think of other tags as well?
//		tags = {
//				@Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc")),
//				@Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2")),
//				@Tag(name = "Tag 3")
//		},
//		externalDocs = @ExternalDocumentation(description = "definition docs desc"),
		servers = {
				@Server(
						description = "Single Cell Expression Atlas local development server",
						url = "http://localhost:8080/gxa/sc"),
				@Server(
						description = "Single Cell Expression Atlas server",
						url = "https://www.ebi.ac.uk/gxa/sc")
		}
)
public class OpenApiSpringBootApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpenApiSpringBootApplication.class, args);
	}
}
