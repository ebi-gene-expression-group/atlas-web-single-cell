package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.model.card.CardModel;
import uk.ac.ebi.atlas.model.card.CardModelAdapter;
import uk.ac.ebi.atlas.utils.GsonProvider;

import static uk.ac.ebi.atlas.model.card.CardIconType.IMAGE;
import static uk.ac.ebi.atlas.utils.UrlHelpers.*;

@RestController
public class JsonHcaLandingPageController extends JsonExceptionHandlingController {
    protected static final String HCA_ACCESSION_PATTERN = "EHCA";

    @GetMapping(value = {"/json/hca"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String jsonHca() {
        return GsonProvider.GSON.toJson(
                ImmutableMap.of("cards", CardModelAdapter.serialize(getHcaCards())));
    }

    private ImmutableList<CardModel> getHcaCards() {
            return ImmutableList.of(
                    CardModel.create(
                            IMAGE,
                            getCustomUrl("/resources/images/logos/hca_cell_logo.png"),
                            ImmutableList.of(
                                    getExperimentLink(
                                            "Mouse cells – Small intestinal epithelium",
                                            "E-EHCA-2"),
                                    getExperimentLink(
                                            "Single cell transcriptome analysis of human pancreas",
                                            "E-GEOD-81547"),
                                    getExperimentLink(
                                            "Single-cell RNA-seq analysis of 1,732 cells throughout a 125-day differentiation protocol that converted H1 human embryonic stem cells to a variety of ventrally-derived cell types",
                                            "E-GEOD-93593"),
                                    getExperimentLink(
                                            "Single-cell RNA-seq analysis of human pancreas from healthy individuals and type 2 diabetes patients",
                                            "E-MTAB-5061"),
                                    getExperimentLink(
                                            "Single-cell transcriptome analysis of precursors of human CD4+ cytotoxic T lymphocytes",
                                            "E-GEOD-106540"),
                                    getExperimentLink(
                                            "Tabula Muris",
                                            "E-ENAD-15"),
                                    getExperimentLink(
                                            "Reconstructing the human first trimester fetal-maternal interface using single cell transcriptomics – 10x data",
                                            "E-MTAB-6701"),
                                    getExperimentLink(
                                            "Reconstructing the human first trimester fetal-maternal interface using single cell transcriptomics – Smartseq 2 data",
                                            "E-MTAB-6678"))));
    }
}

