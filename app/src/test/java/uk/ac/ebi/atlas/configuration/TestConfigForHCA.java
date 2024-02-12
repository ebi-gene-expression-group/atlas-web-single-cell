package uk.ac.ebi.atlas.configuration;

import com.google.common.collect.ImmutableSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import uk.ac.ebi.atlas.hcalandingpage.HcaHumanExperimentDao;
import uk.ac.ebi.atlas.hcalandingpage.HcaHumanExperimentService;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Set;

@Configuration
// Enabling component scanning will also load BasePathsConfig, JdbcConfig and SolrConfig, so just using this class as
// application context is enough in integration tests. It’s important to exclude CacheConfig, otherwise Spring will
// complain if you want to inject classes such as ScxaExperimentTrader, since a proxy will be injected instead! As an
// exercise, remove CacheConfig.class and run tests in ScxaExperimentTraderIT.
@ComponentScan(basePackages = "uk.ac.ebi.atlas",
               excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                                        value = {AppConfig.class, CacheConfig.class}))
public class TestConfigForHCA extends TestConfig {

    private static final String INVALID_CHARACTERISTIC_VALUE = "foo";
    private static final String VALID_CHARACTERISTIC_VALUE = "pancreas";

    private static final ImmutableSet<String> ALL_ACCESSION_IDS =
            ImmutableSet.of("E-CURD-4", "E-EHCA-2", "E-GEOD-71585", "E-GEOD-81547", "E-GEOD-99058", "E-MTAB-5061",
                    "E-ENAD-53");

    @Bean
    public HcaHumanExperimentDao hcaHumanExperimentDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {

        return new HcaHumanExperimentDao(solrCloudCollectionProxyFactory) {

            @Override
            public ImmutableSet<String> fetchExperimentAccessions(String characteristicName,
                                                                  Set<String> characteristicValues) {
                if (characteristicValues.contains(INVALID_CHARACTERISTIC_VALUE)) {
                    return ImmutableSet.of();
                } else if(characteristicValues.contains(VALID_CHARACTERISTIC_VALUE)) {
                    return ImmutableSet.of("E-MTAB-5061");
                } else {
                    return ALL_ACCESSION_IDS;
                }
            }
        };
    }

    @Bean
    public HcaHumanExperimentService hcaHumanExperimentService(ExperimentTrader experimentTrader,
                                                               HcaHumanExperimentDao hcaHumanExperimentDao) {
        return new HcaHumanExperimentService(experimentTrader, hcaHumanExperimentDao);
    }
}
