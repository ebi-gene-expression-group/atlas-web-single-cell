package uk.ac.ebi.atlas.home;


import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

class CellStatsDaoTest {
    private final static String FILTERED_CELLS = "filtered_cells";
    private final static String RAW_CELLS = "raw_cells";

    @Test
    void ifCellStatesFileDoesNotExistReturnUnknownForAllFields() {
        var subject = new CellStatsDao(Paths.get(randomAlphabetic(1, 10)));
        assertThat(subject.cellStatsInformation.get())
                .containsOnly(
                        createMapEntry(FILTERED_CELLS, "unknown"),
                        createMapEntry(RAW_CELLS, "unknown"));
    }

    @Test
    void ifCellStatsFileIsBrokenReturnUnknownForAllFields() {
        var path = createTempFileWithContents(
                "{\n" +
                        "  \"raw_cells\": \"123465\"," +
                        "  \"filtered_cells\":" +
                        "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.cellStatsInformation.get())
                .containsOnly(
                        createMapEntry(FILTERED_CELLS, "unknown"),
                        createMapEntry(RAW_CELLS, "unknown"));
    }

    @Test
    void ifCellStatsFileIsIncompleteReturnUnknownForRemainingFields() {
        var path = createTempFileWithContents(
                "{\n" +
                        "  \"filtered_cells\": \"542312\"" +
                        "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.cellStatsInformation.get())
                .containsOnly(
                        createMapEntry(FILTERED_CELLS, "542312"),
                        createMapEntry(RAW_CELLS, "unknown"));
    }

    @Test
    void cellStatsAreSameAsCellStatsFileContent() {
        var path = createTempFileWithContents(
                "{\n" +
                        "  \"raw_cells\": \"123465\"," +
                        "  \"filtered_cells\": \"5462\"" +
                        "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.cellStatsInformation.get())
                .containsOnly(
                        createMapEntry(FILTERED_CELLS, "5462"),
                        createMapEntry(RAW_CELLS, "123465"));
    }

    private static <K, V> Map.Entry<K, V> createMapEntry(K key, V value) {
        return ImmutableMap.of(key, value).entrySet().iterator().next();
    }

    private static Path createTempFileWithContents(String str) {
        try {
            var path = Files.createTempFile("", "");
            var writer = new BufferedWriter(new FileWriter(path.toFile()));
            writer.write(str);
            writer.flush();
            writer.close();
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}