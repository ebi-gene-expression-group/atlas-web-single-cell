package uk.ac.ebi.atlas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.species.SpeciesFinder;
import uk.ac.ebi.atlas.utils.BioentityIdentifiersReader;

import java.util.HashSet;

@Configuration
@ComponentScan(basePackages = "uk.ac.ebi.atlas")
public class AppConfig {
    private static final int MAX_TIMEOUT_MILLIS = 20000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(MAX_TIMEOUT_MILLIS);
        requestFactory.setConnectTimeout(MAX_TIMEOUT_MILLIS);

        return new RestTemplate(requestFactory);
    }

    @Bean
    public BioentityIdentifiersReader bioentityIdentifiersReader() {
        return new BioentityIdentifiersReader() {
            @Override
            protected int addBioentityIdentifiers(HashSet<String> bioentityIdentifiers, ExperimentType experimentType) {
                return 0;
            }

            @Override
            public HashSet<String> getBioentityIdsFromExperiment(String experimentAccession) {
                return new HashSet<>();
            }

            @Override
            public HashSet<String> getBioentityIdsFromExperiment(String experimentAccession, boolean throwError) {
                return new HashSet<>();
            }
        };
    }

    @Bean
    public SpeciesFinder speciesFinder() {
        return new SpeciesFinder() {};
    }
}
