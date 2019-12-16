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
    private final String dataFilesLocation;

    public SingleCellFilePathConfig(@Value("${data.files.location}") String dataFilesLocation) {
        this.dataFilesLocation = dataFilesLocation;
    }

    @Bean
    public Path cellStatsFilePath() {
        return Paths.get(dataFilesLocation).resolve("scxa").resolve("magetab").resolve("cell_stats.json");
    }
}
