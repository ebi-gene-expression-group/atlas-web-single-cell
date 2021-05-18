package uk.ac.ebi.atlas.home;

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.home.CellStatsDao.CellStatsKey.FILTERED_CELLS;
import static uk.ac.ebi.atlas.home.CellStatsDao.CellStatsKey.RAW_CELLS;

class CellStatsDaoTest {
    private final static ThreadLocalRandom RNG = ThreadLocalRandom.current();
    private final static Long JAVASCRIPT_NUMBER_MAX_SAFE_INTEGER = Double.valueOf(Math.pow(2, 53)).longValue() - 1L;

    @Test
    void ifCellStatsFileDoesNotExistReturn0ForAllFields() {
        var subject = new CellStatsDao(Paths.get(randomAlphabetic(1, 10)));

        assertThat(subject.get(RAW_CELLS)).isEqualTo(0L);
        assertThat(subject.get(FILTERED_CELLS)).isEqualTo(0L);
    }

    @Test
    void ifCellStatsFileIsBrokenReturn0ForAllFields() {
        var path = createTempFileWithContents(
                "{" +
                  "\"raw_cells\": 123465," +
                  "\"filtered_cells\":" +
                "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.get(RAW_CELLS)).isEqualTo(0L);
        assertThat(subject.get(FILTERED_CELLS)).isEqualTo(0L);
    }

    @Test
    void ifCellStatsFileIsIncompleteReturn0ForRemainingFields() {
        var randomNumberOfCells = RNG.nextLong(1, JAVASCRIPT_NUMBER_MAX_SAFE_INTEGER);
        var path = createTempFileWithContents(
                "{" +
                  "\"filtered_cells\": " + randomNumberOfCells +
                "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.get(RAW_CELLS)).isEqualTo(0L);
        assertThat(subject.get(FILTERED_CELLS)).isEqualTo(randomNumberOfCells);
    }

    @Test
    void ifANameIsRepeatedKeepTheLastValue() {
        var randomNumberOfRawCells = RNG.nextLong(1, JAVASCRIPT_NUMBER_MAX_SAFE_INTEGER);
        var randomNumberOfFilteredCells = RNG.nextLong(1, randomNumberOfRawCells + 1);
        var path = createTempFileWithContents(
                "{" +
                  "\"raw_cells\": " + RNG.nextLong(1, Long.MAX_VALUE) + "," +
                  "\"filtered_cells\": " + RNG.nextLong(1, Long.MAX_VALUE) + "," +
                   "\"raw_cells\": " + randomNumberOfRawCells + "," +
                   "\"filtered_cells\": " + randomNumberOfFilteredCells +
                "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.get(RAW_CELLS)).isEqualTo(randomNumberOfRawCells);
        assertThat(subject.get(FILTERED_CELLS)).isEqualTo(randomNumberOfFilteredCells);
    }

    @Test
    void ifValueIsNotANumberReturn0() {
        var path = createTempFileWithContents(
                "{" +
                        "\"raw_cells\": \"" + randomAlphanumeric(10) + "\"," +
                        "\"filtered_cells\": []" +
                        "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.get(RAW_CELLS)).isEqualTo(0L);
        assertThat(subject.get(FILTERED_CELLS)).isEqualTo(0L);
    }

    @Test
    void cellStatsAreParsedCorrectly() {
        var randomNumberOfRawCells = RNG.nextLong(1, JAVASCRIPT_NUMBER_MAX_SAFE_INTEGER);
        var randomNumberOfFilteredCells = RNG.nextLong(1, randomNumberOfRawCells + 1);
        var path = createTempFileWithContents(
                "{" +
                  "\"raw_cells\": " + randomNumberOfRawCells + "," +
                  "\"filtered_cells\": " + randomNumberOfFilteredCells +
                "}");

        var subject = new CellStatsDao(path);
        assertThat(subject.get(RAW_CELLS)).isEqualTo(randomNumberOfRawCells);
        assertThat(subject.get(FILTERED_CELLS)).isEqualTo(randomNumberOfFilteredCells);
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