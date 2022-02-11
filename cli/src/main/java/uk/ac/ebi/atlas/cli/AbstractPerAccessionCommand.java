package uk.ac.ebi.atlas.cli;

import picocli.CommandLine;
import uk.ac.ebi.atlas.cli.utils.AccessionsWriter;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractPerAccessionCommand {

    private static final Logger LOGGER = Logger.getLogger(AbstractPerAccessionCommand.class.getName());;

    @CommandLine.Option(names = {"-f", "--failed-accessions-path"}, description = "File to write failed accessions to.", required = false)
    protected String failedOutputPath;

    @CommandLine.Option(names = {"-e", "--experiment"}, split = ",", description = "one or more experiment accessions", required = true)
    protected List<String> experimentAccessions;

    protected int handleFailedAccessions(Collection<String> failedAccessions) {
        int status = 0;
        if (!failedAccessions.isEmpty()) {
            status = 1;
            LOGGER.warning(String.format("%s experiments failed", failedAccessions.size()));
            LOGGER.info(String.format("Re-run with the following arguments to re-try failed accessions: %s", String.join(",", failedAccessions)));
            if (failedOutputPath != null) {
                AccessionsWriter writer = new AccessionsWriter(failedOutputPath, failedAccessions);
                writer.write();
            }
        }
        return status;
    }
}
