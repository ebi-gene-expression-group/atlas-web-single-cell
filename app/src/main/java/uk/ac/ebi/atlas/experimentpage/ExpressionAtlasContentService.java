package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.link.*;
import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;

@Component
public class ExpressionAtlasContentService {

    private final LinkToEna linkToEna;
    private final LinkToEga linkToEga;
    private final LinkToGeo linkToGeo;
    private final LinkToArrayExpress linkToArrayExpress;
    private final ExperimentTrader experimentTrader;

    public ExpressionAtlasContentService(
            LinkToArrayExpress linkToArrayExpress,
            LinkToEna linkToEna,
            LinkToEga linkToEga,
            LinkToGeo linkToGeo,
            ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;
        this.linkToEna = linkToEna;
        this.linkToEga = linkToEga;
        this.linkToGeo = linkToGeo;
        this.linkToArrayExpress = linkToArrayExpress;
    }

    public List<ExternallyAvailableContent> getExternalResourceLinks(String experimentAccession,
                                                 String accessKey,
                                                 ExternallyAvailableContent.ContentType contentType) {
        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        var externalResourceLinks = ImmutableList.builder();

        if (experimentAccession.matches("E-MTAB.*|E-ERAD.*|E-GEUV.*")) {
            externalResourceLinks.addAll(linkToArrayExpress.get(experiment));
        }
        var otherExternalResourceLinks =  externalResourceLinks(experiment);
        arrayExpressResourceLinks.addAll(otherExternalResourceLinks.build());

        return arrayExpressResourceLinks.build();
    }

    private ImmutableList.Builder<ExternallyAvailableContent> externalResourceLinks(Experiment<?> experiment) {
        ImmutableList.Builder<ExternallyAvailableContent> otherExternalResourceLinks = ImmutableList.builder();

        Map<String, List<String>> resourceList = experiment.getSecondaryAccessions().stream()
                .collect(Collectors.groupingBy(accession -> {
                    if (accession.matches("GSE.*")) return "GEO";
                    if (accession.matches("EGA.*")) return "EGA";
                    if (accession.matches("PDX.*")) return "PRIDE";
                    if (accession.matches("ERP.*|SRP.*|DRP.*|PRJEB.*|PRJNA.*|PRJDB.*")) return "ENA";
                    return "OTHER";
                }));

        resourceList.entrySet().removeIf(entry -> {
            String resource = entry.getKey();
            var accessions = entry.getValue().stream()
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

            switch (resource) {
                case "GEO":
                    otherExternalResourceLinks.addAll(linkToGeo.get(experiment));
                    break;
                case "EGA":
                    otherExternalResourceLinks.addAll(linkToEga.get(experiment));
                    break;
                case "ENA":
                    otherExternalResourceLinks.addAll(linkToEna.get(experiment));
                    break;
                case "OTHER":
                    // Remove this entry by returning true
                    return true;
            }

            // Update the entry's value
            entry.setValue(accessions);
            return false;
        });

        return otherExternalResourceLinks;
    }
}
