package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.link.LinkToArrayExpress;
import uk.ac.ebi.atlas.experimentpage.link.LinkToEga;
import uk.ac.ebi.atlas.experimentpage.link.LinkToEna;
import uk.ac.ebi.atlas.experimentpage.link.LinkToGeo;
import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.List;
import static com.google.common.collect.ImmutableList.toImmutableList;

@Component
public class ExpressionAtlasContentService {

    private final ExternallyAvailableContentService<SingleCellBaselineExperiment>
            singleCellBaselineExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<SingleCellBaselineExperiment>
            singleCellBaselineExperimentExternallyAvailableContentServiceGeo;
    private final ExternallyAvailableContentService<SingleCellBaselineExperiment>
            singleCellBaselineExperimentExternallyAvailableContentServiceEna;
    private final ExternallyAvailableContentService<SingleCellBaselineExperiment>
            singleCellBaselineExperimentExternallyAvailableContentServiceEga;
    private final ExperimentTrader experimentTrader;

    public ExpressionAtlasContentService(
            LinkToArrayExpress.SingleCell singleCellLinkToArrayExpress
            ,
            LinkToEna.SingleCell singleCellLinkToEna,
            LinkToEga.SingleCell singleCellLinkToEga,
            LinkToGeo.SingleCell singleCellLinkToGeo,
            ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;

        this.singleCellBaselineExperimentExternallyAvailableContentService=
                new ExternallyAvailableContentService<>(ImmutableList.of(singleCellLinkToArrayExpress));

        this.singleCellBaselineExperimentExternallyAvailableContentServiceGeo =
                new ExternallyAvailableContentService<>(ImmutableList.of(singleCellLinkToGeo));

        this.singleCellBaselineExperimentExternallyAvailableContentServiceEna =
                new ExternallyAvailableContentService<>(ImmutableList.of(singleCellLinkToEna));

        this.singleCellBaselineExperimentExternallyAvailableContentServiceEga =
                new ExternallyAvailableContentService<>(ImmutableList.of(singleCellLinkToEga));
    }

    public List<ExternallyAvailableContent> list(String experimentAccession,
                                                 String accessKey,
                                                 ExternallyAvailableContent.ContentType contentType) {
        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        String externalResourceType = externalResourceLinksPriority(experiment);

        List list = singleCellBaselineExperimentExternallyAvailableContentService.list(
                (SingleCellBaselineExperiment) experiment, contentType);
        List list2 =  externalResourceType.equals("geo") ?
                singleCellBaselineExperimentExternallyAvailableContentServiceGeo.list(
                (SingleCellBaselineExperiment) experiment, contentType) :
                externalResourceType.equals("ega") ?
                        singleCellBaselineExperimentExternallyAvailableContentServiceEga.list(
                        (SingleCellBaselineExperiment) experiment, contentType) :
                        singleCellBaselineExperimentExternallyAvailableContentServiceEna.list(
                        (SingleCellBaselineExperiment) experiment, contentType);
        list2.addAll(list);
        return list2;
    }

    private String externalResourceLinksPriority(Experiment<?> experiment) {
        var geoAccessions = experiment.getSecondaryAccessions().stream()
                .filter(accession -> accession.matches("GSE.*"))
                .collect(toImmutableList());
        var egaAccessions = experiment.getSecondaryAccessions().stream()
                .filter(accession -> accession.matches("EGA.*"))
                .collect(toImmutableList());

        return geoAccessions.isEmpty() ? egaAccessions.isEmpty() ? "ena" : "ega" : "geo";

    }
}
