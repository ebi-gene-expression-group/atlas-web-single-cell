package uk.ac.ebi.atlas.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experiments.ExperimentInfoListService;
import uk.ac.ebi.atlas.utils.ExperimentInfo;

import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EFO;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EG;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.ENSEMBL;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.WBPS;

@Controller
public class HomeController extends HtmlExceptionHandlingController {
    private final LatestExperimentsService latestExperimentsService;
    private final ExperimentInfoListService experimentInfoListService;
    private final AtlasInformationDao atlasInformationDao;
    private final CellStatsDao cellStatsDao;

    public HomeController(LatestExperimentsService latestExperimentsService,
                          ExperimentInfoListService experimentInfoListService,
                          AtlasInformationDao atlasInformationDao,
                          CellStatsDao cellStatsDao) {
        this.latestExperimentsService = latestExperimentsService;
        this.experimentInfoListService = experimentInfoListService;
        this.atlasInformationDao = atlasInformationDao;
        this.cellStatsDao = cellStatsDao;
    }

    @RequestMapping(value = "/home")
    public String getHomePage(Model model) {
        model.addAllAttributes(latestExperimentsService.fetchLatestExperimentsAttributes());

        model.addAttribute("numberOfStudies", experimentInfoListService.listPublicExperiments().size());

        long numberOfSpecies =
                experimentInfoListService.listPublicExperiments().stream()
                        .map(ExperimentInfo::getSpecies)
                        .distinct()
                        .count();
        model.addAttribute("numberOfSpecies", numberOfSpecies);

        model.addAttribute("numberOfCells",
                cellStatsDao.cellStatsInformation
                        .get()
                        .get("filtered_cells"));

        model.addAttribute("info", atlasInformationDao.atlasInformation.get());
        model.addAttribute("ensembl", ENSEMBL.getId());
        model.addAttribute("genomes", EG.getId());
        model.addAttribute("paraSite", WBPS.getId());
        model.addAttribute("efo", EFO.getId());

        return "home";
    }
}
