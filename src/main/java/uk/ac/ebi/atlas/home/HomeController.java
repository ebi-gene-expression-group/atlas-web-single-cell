package uk.ac.ebi.atlas.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experiments.ExperimentInfoListService;
import uk.ac.ebi.atlas.home.species.SpeciesSummaryService;

import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EFO;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EG;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.ENSEMBL;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.WBPS;
import static uk.ac.ebi.atlas.home.CellStatsDao.CellStatsKey.FILTERED_CELLS;

@Controller
public class HomeController extends HtmlExceptionHandlingController {
    private final LatestExperimentsService latestExperimentsService;
    private final ExperimentInfoListService experimentInfoListService;
    private final SpeciesSummaryService speciesSummaryService;
    private final CellStatsDao cellStatsDao;
    private final AtlasInformationDao atlasInformationDao;

    public HomeController(LatestExperimentsService latestExperimentsService,
                          ExperimentInfoListService experimentInfoListService,
                          SpeciesSummaryService speciesSummaryService,
                          CellStatsDao cellStatsDao,
                          AtlasInformationDao atlasInformationDao) {
        this.latestExperimentsService = latestExperimentsService;
        this.experimentInfoListService = experimentInfoListService;
        this.speciesSummaryService = speciesSummaryService;
        this.cellStatsDao = cellStatsDao;
        this.atlasInformationDao = atlasInformationDao;
    }

    @RequestMapping(value = "/home")
    public String getHomePage(Model model) {
        model.addAllAttributes(latestExperimentsService.fetchLatestExperimentsAttributes());

        model.addAttribute("numberOfStudies", experimentInfoListService.listPublicExperiments().size());
        model.addAttribute("numberOfSpecies", speciesSummaryService.getReferenceSpecies().size());
        model.addAttribute("numberOfCells", cellStatsDao.get(FILTERED_CELLS));

        model.addAttribute("info", atlasInformationDao.atlasInformation.get());
        model.addAttribute("ensembl", ENSEMBL.getId());
        model.addAttribute("genomes", EG.getId());
        model.addAttribute("paraSite", WBPS.getId());
        model.addAttribute("efo", EFO.getId());

        return "home";
    }
}
