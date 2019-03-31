package uk.ac.ebi.atlas.experimentimport.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrud;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class SingleCellOpsExecutionService implements ExperimentOpsExecutionService {
    private final ExperimentCrud experimentCrud;

    public SingleCellOpsExecutionService(ExperimentCrud experimentCrud) {
        this.experimentCrud = experimentCrud;
    }

    private Stream<ExperimentDto> allDtos() {
        return experimentCrud.findAllExperiments().stream()
                .filter(experimentDTO -> experimentDTO.getExperimentType().isSingleCell());
    }

    @Override
    public List<String> findAllExperiments() {
        return allDtos().map(ExperimentDto::getExperimentAccession).collect(toList());
    }

    @Override
    public Optional<JsonElement> attemptExecuteOneStatelessOp(String accession, Op op) {
        switch (op) {
            case LIST:
                return Optional.of(experimentCrud.findExperiment(accession).toJson());
            default:
                return Optional.empty();
        }
    }

    @Override
    public Optional<? extends List<Pair<String, ? extends JsonElement>>>
    attemptExecuteForAllAccessions(Collection<Op> ops) {
        if (ops.equals(Collections.singleton(Op.LIST))) {
            return Optional.of(list());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<? extends List<Pair<String, ? extends JsonElement>>> attemptExecuteForAllAccessions(Op op) {
        if (op.equals(Op.LIST)) {
            return Optional.of(list());
        } else {
            return Optional.empty();
        }
    }

    private List<Pair<String, ? extends JsonElement>> list() {
        return allDtos()
                .map((Function<ExperimentDto, Pair<String, ? extends JsonElement>>) experimentDTO ->
                        Pair.of(experimentDTO.getExperimentAccession(), experimentDTO.toJson())).collect(toList());
    }

    @Override
    public JsonPrimitive attemptExecuteStatefulOp(String accession, Op op) {
        JsonPrimitive resultOfTheOp = ExperimentOps.DEFAULT_SUCCESS_RESULT;
        boolean isPrivate = true;
        switch (op) {
            case UPDATE_PRIVATE:
                experimentCrud.makeExperimentPrivate(accession);
                break;
            case UPDATE_PUBLIC:
                experimentCrud.makeExperimentPublic(accession);
                break;
            case UPDATE_DESIGN:
                experimentCrud.updateSingleCellExperimentDesign(accession);
                break;
            case IMPORT_PUBLIC:
                isPrivate = false;
            case IMPORT:
                UUID accessKeyUUID = experimentCrud.importSingleCellExperiment(accession, isPrivate);
                resultOfTheOp = new JsonPrimitive("success, access key UUID: " + accessKeyUUID);
                break;
            case DELETE:
                experimentCrud.deleteExperiment(accession);
                break;

            default:
                throw new RuntimeException("Operation not supported: " + op.name());
        }
        return resultOfTheOp;
    }
}
