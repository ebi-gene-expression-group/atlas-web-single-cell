package uk.ac.ebi.atlas.cli.experimentDesign;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import uk.ac.ebi.atlas.experimentimport.ScxaExperimentCrud;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "update-experiment-design",
        description = "Update experiment design for a set of accessions")
public class ExperimentDesignCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ExperimentDesignCommand.class.getName());

    @Option(names = {"-e", "--experiment"}, split = ",", description = "one or more experiment accessions", required = true)
    private List<String> experimentAccessions;

    private final ScxaExperimentCrud experimentCrud;

    public ExperimentDesignCommand(ScxaExperimentCrud scxaExperimentCrud) {
        this.experimentCrud = scxaExperimentCrud;
    }

    @Override
    public Integer call() {

        LOGGER.info("Starting update experiment designs for accessions.");
        experimentAccessions.stream()
                .forEach(accession -> experimentCrud.updateExperimentDesign(accession));

        return 0;
    }
}
