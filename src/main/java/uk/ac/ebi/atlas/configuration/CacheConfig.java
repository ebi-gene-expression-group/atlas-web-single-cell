package uk.ac.ebi.atlas.configuration;

import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

                builder -> builder.name("jsonExperimentMetadata"),
                builder -> builder.name("jsonExperimentPageTabs"),
                builder -> builder.name("cellCounts"),
                builder -> builder.name("expectedClusters"),
                builder -> builder.name("minimumMarkerProbability"),

                builder -> builder.name("jsonCellMetadata"),
                builder -> builder.name("jsonTSnePlotWithClusters"),
                builder -> builder.name("jsonTSnePlotWithMetadata"),

                builder -> builder.name("hcaMetadata"));
    }
}
