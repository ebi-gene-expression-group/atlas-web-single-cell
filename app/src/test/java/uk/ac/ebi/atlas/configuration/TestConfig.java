package uk.ac.ebi.atlas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.species.SpeciesFinder;
import uk.ac.ebi.atlas.utils.BioentityIdentifiersReader;

import java.util.HashSet;

@Configuration
// Enabling component scanning will also load BasePathsConfig, JdbcConfig and SolrConfig, so just using this class as
// application context is enough in integration tests. It’s important to exclude CacheConfig, otherwise Spring will
// complain if you want to inject classes such as ScxaExperimentTrader, since a proxy will be injected instead! As an
// exercise, remove CacheConfig.class and run tests in ScxaExperimentTraderIT.
@ComponentScan(basePackages = "uk.ac.ebi.atlas",
               excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                                        value = {AppConfig.class, CacheConfig.class}))
public class TestConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
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
