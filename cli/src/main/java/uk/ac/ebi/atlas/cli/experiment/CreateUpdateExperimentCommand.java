package uk.ac.ebi.atlas.cli.experiment;

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
        name = "create-update-experiment",
        description = "Creates or updates an experiment")
public class CreateUpdateExperimentCommand extends AbstractPerAccessionCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(CreateUpdateExperimentCommand.class.getName());

    @Option(names = {"-p", "--private-experiment"}, split = ",", description = "one or more experiment accessions to be " +
            "loaded/updated as private.")
    private List<String> privateExperimentAccessions = new ArrayList<>();

    private final ScxaExperimentCrud experimentCrud;

    public CreateUpdateExperimentCommand(ScxaExperimentCrud scxaExperimentCrud) {
        this.experimentCrud = scxaExperimentCrud;
    }

    @Override
    public Integer call() {

        if (experimentAccessions.isEmpty() && privateExperimentAccessions.isEmpty()) {
            LOGGER.severe("At least either one experiment (-e) or private experiment (-p) needs to be supplied");
            return 1;
        }
        LOGGER.info("Starting loading/updating experiments:");
        var accessions = Stream.concat(experimentAccessions.stream(), privateExperimentAccessions.stream())
                .collect(Collectors.toSet());
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
