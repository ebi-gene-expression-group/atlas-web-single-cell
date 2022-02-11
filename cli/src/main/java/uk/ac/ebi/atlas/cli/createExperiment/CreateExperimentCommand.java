package uk.ac.ebi.atlas.cli.createExperiment;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.ac.ebi.atlas.cli.AbstractPerAccessionCommand;
import uk.ac.ebi.atlas.experimentimport.ScxaExperimentCrud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Command(
        name = "update-experiment-design",
        description = "Update experiment design for a set of accessions")
public class CreateExperimentCommand extends AbstractPerAccessionCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(CreateExperimentCommand.class.getName());

    @Option(names = {"-p", "--private-experiment"}, split = ",", description = "one or more experiment accessions to be loaded as private", required = true)
    private List<String> privateExperimentAccessions;

    private final ScxaExperimentCrud experimentCrud;

    public CreateExperimentCommand(ScxaExperimentCrud scxaExperimentCrud) {
        this.experimentCrud = scxaExperimentCrud;
    }

    @Override
    public Integer call() {

        LOGGER.info("Starting loading experiments:");
        var accessions = Stream.concat(experimentAccessions.stream(), privateExperimentAccessions.stream())
                .collect(Collectors.toList());
        var failedAccessions = new ArrayList<String>();
        for (var accession : accessions) {
            LOGGER.info(String.format("Loading %s", accession));
            try {
                var accessKeyUUID = experimentCrud.createExperiment(accession, privateExperimentAccessions.contains(accession));
                LOGGER.info("success, access key UUID: " + accessKeyUUID);
            } catch (RuntimeException e) {
                LOGGER.severe(String.format("Could not load %s due to %s", accession, e.getMessage()));
                failedAccessions.add(accession);
            }
        }

        return handleFailedAccessions(failedAccessions);
    }
}
