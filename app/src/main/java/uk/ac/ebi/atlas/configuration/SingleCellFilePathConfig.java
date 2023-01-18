package uk.ac.ebi.atlas.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@PropertySource("classpath:configuration.properties")
public class SingleCellFilePathConfig {
    private final String experimentFilesLocation;

    public SingleCellFilePathConfig(@Value("${experiment.files.location}") String experimentFilesLocation) {
        this.experimentFilesLocation = experimentFilesLocation;
    }

    @Bean
    public Path cellStatsFilePath() {
        return Paths.get(experimentFilesLocation).resolve("magetab").resolve("cell_stats.json");
    }
}
