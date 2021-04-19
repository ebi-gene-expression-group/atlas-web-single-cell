package uk.ac.ebi.atlas.experimentimport.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;
import uk.ac.ebi.atlas.experimentimport.ScxaExperimentCrud;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.ac.ebi.atlas.experimentimport.admin.Op.LIST;

@Component
public class SingleCellOpsExecutionService implements ExperimentOpsExecutionService {
    private final ScxaExperimentCrud scxaExperimentCrud;

    public SingleCellOpsExecutionService(ScxaExperimentCrud scxaExperimentCrud) {
        this.scxaExperimentCrud = scxaExperimentCrud;
    }

    private Stream<ExperimentDto> allDtos() {
        return scxaExperimentCrud.readExperiments().stream()
                .filter(experimentDTO -> experimentDTO.getExperimentType().isSingleCell());
    }

    @Override
    public List<String> findAllExperiments() {
        return allDtos().map(ExperimentDto::getExperimentAccession).collect(toList());
    }

    @Override
    public Optional<JsonElement> attemptExecuteOneStatelessOp(String accession, Op op) {
        return (op.equals(LIST))
                ? scxaExperimentCrud.readExperiment(accession).map(ExperimentDto::toJson)
                : Optional.empty();
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
                scxaExperimentCrud.updateExperimentPrivate(accession, true);
                break;
            case UPDATE_PUBLIC:
                scxaExperimentCrud.updateExperimentPrivate(accession, false);
                break;
            case UPDATE_DESIGN:
                scxaExperimentCrud.updateExperimentDesign(accession);
                break;
            case IMPORT_PUBLIC:
                isPrivate = false;
            case IMPORT:
                UUID accessKeyUUID = scxaExperimentCrud.createExperiment(accession, isPrivate);
                resultOfTheOp = new JsonPrimitive("success, access key UUID: " + accessKeyUUID);
                break;
            case DELETE:
                scxaExperimentCrud.deleteExperiment(accession);
                break;

            default:
                throw new RuntimeException("Operation not supported: " + op.name());
        }
        return resultOfTheOp;
    }
}
