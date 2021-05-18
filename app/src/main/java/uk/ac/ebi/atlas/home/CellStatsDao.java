package uk.ac.ebi.atlas.home;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.atlassian.util.concurrent.LazyReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

@Component
public class CellStatsDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(CellStatsDao.class);

    public enum CellStatsKey {
        RAW_CELLS("raw_cells"),
        FILTERED_CELLS("filtered_cells");

        private final String keyName;

        CellStatsKey(String keyName) {
            this.keyName = keyName;
        }
    }

    private static class CellStats {
        private final HashMap<CellStatsKey, Long> keyValues = new HashMap<>();

        @JsonAnySetter
        public void add(String key, long value) {
            LOGGER.debug("{}: {}", key, value);
            Arrays.stream(CellStatsKey.values())
                    .filter(cellStatsKey -> cellStatsKey.keyName.equals(key))
                    .findFirst()
                    .ifPresent(cellStatsKey -> keyValues.put(cellStatsKey, value));
        }

        private long get(CellStatsKey cellStatsKey) {
            return keyValues.getOrDefault(cellStatsKey, 0L);
        }
    }

    private final Path cellStatsFilePath;

    private final LazyReference<CellStats> cellStatsInformation =
            new LazyReference<>() {
                @Override
                protected CellStats create() {
                    try (var reader = Files.newBufferedReader(cellStatsFilePath, StandardCharsets.UTF_8)) {
                        LOGGER.info("Cell stats file found {}:", cellStatsFilePath.toString());
                        return new ObjectMapper().readerFor(CellStats.class).readValue(reader);
                    } catch (Exception e) {
                        LOGGER.error("Error reading cell stats file: {}", e.getMessage());
                    }
                    return new CellStats();
                }
            };

    public CellStatsDao(Path cellStatsFilePath) {
        this.cellStatsFilePath = cellStatsFilePath;
    }

    public long get(CellStatsKey cellStatsKey) {
        return cellStatsInformation.get().get(cellStatsKey);
    }
}
