package uk.ac.ebi.atlas.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication(scanBasePackages = "uk.ac.ebi.atlas")
public class ExpressionAtlasCliApplication implements CommandLineRunner, ExitCodeGenerator {
    private final CommandLine.IFactory factory;
    private final ExpressionAtlasCliCommand expressionAtlasCliCommand;

    private int exitCode;

    public ExpressionAtlasCliApplication(CommandLine.IFactory factory,
                                         ExpressionAtlasCliCommand expressionAtlasCliCommand) {
        this.factory = factory;
        this.expressionAtlasCliCommand = expressionAtlasCliCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ExpressionAtlasCliApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(expressionAtlasCliCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
