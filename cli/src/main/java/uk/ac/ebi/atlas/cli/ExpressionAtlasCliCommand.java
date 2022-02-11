package uk.ac.ebi.atlas.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import uk.ac.ebi.atlas.cli.createExperiment.CreateExperimentCommand;
import uk.ac.ebi.atlas.cli.experimentDesign.ExperimentDesignCommand;

@Command(subcommands = {
        ExperimentDesignCommand.class,
        CreateExperimentCommand.class
})
@Component
public class ExpressionAtlasCliCommand {
}