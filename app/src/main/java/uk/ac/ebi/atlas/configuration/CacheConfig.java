package uk.ac.ebi.atlas.configuration;

import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!dev")
@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new SpringCache2kCacheManager().addCaches(
                builder -> builder.name("designElementsByGeneId"),
                builder -> builder.name("arrayDesignByAccession"),
                builder -> builder.name("bioentityProperties"),

                builder -> builder.name("experiment"),
                builder -> builder.name("experimentAttributes"),
                builder -> builder.name("speciesSummary"),
                // Spring unwraps Optional types
                builder -> builder.name("experimentCollections").permitNullValues(true),
                builder -> builder.name("experiment2Collections"),

                builder -> builder.name("jsonExperimentsList"),
                builder -> builder.name("jsonExperimentMetadata"),
                builder -> builder.name("jsonExperimentPageTabs"),
                builder -> builder.name("cellCounts"),
                // We need null values for Optional; see https://github.com/cache2k/cache2k/issues/141
                builder -> builder.name("expectedClusters").permitNullValues(true),
                builder -> builder.name("minimumMarkerProbability"),

                builder -> builder.name("jsonCellMetadata"),
                builder -> builder.name("jsonTSnePlotWithClusters"),
                builder -> builder.name("jsonTSnePlotWithMetadata"),

                builder -> builder.name("hcaMetadata"),
                builder -> builder.name("inferredCellTypesOntology"),
                builder -> builder.name("inferredCellTypesAuthors"));
    }
}
