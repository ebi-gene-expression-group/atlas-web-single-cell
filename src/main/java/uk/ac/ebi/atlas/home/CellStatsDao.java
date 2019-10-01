package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.stream.JsonReader;
import io.atlassian.util.concurrent.LazyReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Component
public class CellStatsDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(CellStatsDao.class);
    private final static String FILTERED_CELLS = "filtered_cells";
    private final static String RAW_CELLS = "raw_cells";

    private Path cellStatsFilePath;
    private HashMap<String, String> cellStats =
            Maps.newHashMap();

    public final LazyReference<ImmutableMap<String, String>> cellStatsInformation =
            new LazyReference<>() {
                @Override
                protected ImmutableMap<String, String> create() {
                    try {
                        //default value of map
                        cellStats.put(FILTERED_CELLS, "unknown");
                        cellStats.put(RAW_CELLS, "unknown");

                        var jsonReader =
                                new JsonReader(
                                        Files.newBufferedReader(cellStatsFilePath, StandardCharsets.UTF_8));
                        LOGGER.info("Cell stats file found {}:", cellStatsFilePath.toString());

                        cellStats.putAll(GSON.<HashMap<String, String>>fromJson(jsonReader, HashMap.class));
                        LOGGER.info("{}", cellStats.toString());

                        jsonReader.close();
                    } catch (Exception e) {
                        LOGGER.error("Error reading cell stats file: {}", e.getMessage());
                    }
                    return ImmutableMap.copyOf(cellStats);
                }
            };

    public CellStatsDao(Path cellStatsFilePath) {
        this.cellStatsFilePath = cellStatsFilePath;
    }
}
