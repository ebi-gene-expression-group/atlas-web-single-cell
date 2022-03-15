package uk.ac.ebi.atlas.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import uk.ac.ebi.atlas.cli.experiment.CreateUpdateExperimentCommand;
import uk.ac.ebi.atlas.cli.experiment.ExperimentDesignCommand;

@Command(subcommands = {
        ExperimentDesignCommand.class,
        CreateUpdateExperimentCommand.class
})
@Component
public class ExpressionAtlasCliCommand {
}