package uk.ac.ebi.atlas.experimentimport.admin;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.experimentimport.ScxaExperimentCrud;
import uk.ac.ebi.atlas.resource.DataFileHub;

@Controller
@Scope("request")
@RequestMapping("/admin/experiments")
public class SingleCellExperimentAdminController extends ExperimentAdminController {
    public SingleCellExperimentAdminController(DataFileHub dataFileHub,
                                               ScxaExperimentCrud scxaExperimentCrud) {
        super(
                new ExperimentOps(
                        new ExperimentOpLogWriter(dataFileHub),
                        new SingleCellOpsExecutionService(scxaExperimentCrud)));
    }
}
