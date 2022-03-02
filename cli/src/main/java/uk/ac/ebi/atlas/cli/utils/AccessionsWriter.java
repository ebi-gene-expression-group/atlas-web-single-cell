package uk.ac.ebi.atlas.cli.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Writes a collection of accessions to a specific file path. For use in recovery patterns of workflows that use the CLI.
 */
public class AccessionsWriter {

    private final String path;
    private final Collection<String> accessions;
    private static final Logger LOGGER = Logger.getLogger(AccessionsWriter.class.getName());

    /**
     * The accession writer requires a path for writing and a collection of accessions.
     *
     * @param path to the file where accessions will be written
     * @param accessions a collection of accessions to write
     */
    public AccessionsWriter(String path, Collection<String> accessions) {
        this.path = path;
        this.accessions = accessions;
    }

    /**
     * Writes all accessions given at once.
     */
    public void write() {
        try {
            Files.write(Paths.get(path), accessions);
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
            System.exit(2);
        }
    }
}
