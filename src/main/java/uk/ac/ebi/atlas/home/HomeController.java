package uk.ac.ebi.atlas.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EFO;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EG;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.ENSEMBL;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.WBPS;
import static uk.ac.ebi.atlas.home.CellStatsDao.CellStatsKey.FILTERED_CELLS;

@Controller
public class HomeController extends HtmlExceptionHandlingController {
    private final LatestExperimentsService latestExperimentsService;
    private final ExperimentTrader experimentTrader;
    private final AtlasInformationDao atlasInformationDao;
    private final CellStatsDao cellStatsDao;

    public HomeController(LatestExperimentsService latestExperimentsService,
                          ExperimentTrader experimentTrader,
                          AtlasInformationDao atlasInformationDao,
                          CellStatsDao cellStatsDao) {
        this.latestExperimentsService = latestExperimentsService;
        this.experimentTrader = experimentTrader;
        this.atlasInformationDao = atlasInformationDao;
        this.cellStatsDao = cellStatsDao;
    }

    @RequestMapping(value = "/home")
    public String getHomePage(Model model) {
        model.addAllAttributes(latestExperimentsService.fetchLatestExperimentsAttributes());

        model.addAttribute("numberOfStudies", experimentTrader.getPublicExperiments().size());

        long numberOfSpecies =
                experimentTrader.getPublicExperiments().stream()
                        .map(Experiment::getSpecies)
                        .distinct()
                        .count();
        model.addAttribute("numberOfSpecies", numberOfSpecies);

        model.addAttribute("numberOfCells", cellStatsDao.get(FILTERED_CELLS));

        model.addAttribute("info", atlasInformationDao.atlasInformation.get());
        model.addAttribute("ensembl", ENSEMBL.getId());
        model.addAttribute("genomes", EG.getId());
        model.addAttribute("paraSite", WBPS.getId());
        model.addAttribute("efo", EFO.getId());

        return "home";
    }
}
