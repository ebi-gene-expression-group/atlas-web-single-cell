package uk.ac.ebi.atlas.experimentimport;

import com.google.common.collect.Sets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ScxaExperimentDao extends ExperimentDao {
    // Create
    private static final String INSERT_NEW_EXPERIMENT =
            "INSERT INTO scxa_experiment " +
                    "(accession, type, species, private, access_key, pubmed_ids, dois) VALUES (?, ?, ?, ?, ?, ?, ?)";
    // Read
    private static final String SELECT_EXPERIMENT_AS_ADMIN_BY_ACCESSION =
            "SELECT * FROM scxa_experiment WHERE accession=?";
    private static final String SELECT_EXPERIMENT_BY_ACCESSION_AND_ACCESS_KEY =
            "SELECT * FROM scxa_experiment WHERE accession=? AND (private IS FALSE OR access_key=?)";
    private static final String SELECT_PUBLIC_EXPERIMENTS = "SELECT accession FROM scxa_public_experiment";
    private static final String SELECT_ALL_EXPERIMENTS_AS_ADMIN = "SELECT * FROM scxa_experiment";
    private static final String COUNT_EXPERIMENTS = "SELECT COUNT(*) FROM scxa_experiment";

    // Update
    private static final String UPDATE_EXPERIMENT = "UPDATE scxa_experiment SET private=? WHERE accession=?";
    // Delete
    private static final String DELETE_EXPERIMENT = "DELETE FROM scxa_experiment WHERE accession=?";

    public ScxaExperimentDao(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(jdbcTemplate, namedParameterJdbcTemplate);
    }

    // Will fail when trying to add experiment already present in database
    @Override
    public void addExperiment(ExperimentDto experimentDto, UUID accessKeyUuid) {
        // Add experiment row
        jdbcTemplate.update(
                INSERT_NEW_EXPERIMENT,
                experimentDto.getExperimentAccession(),
                experimentDto.getExperimentType().name(),
                experimentDto.getSpecies(),
                experimentDto.isPrivate(),
                accessKeyUuid.toString(),
                getPublicationIdsAsString(experimentDto.getPubmedIds()),
                getPublicationIdsAsString(experimentDto.getDois()));
    }

    @Override
    public HashSet<String> findPublicExperimentAccessions(ExperimentType... experimentTypes) {
        return Sets.newHashSet(jdbcTemplate.queryForList(SELECT_PUBLIC_EXPERIMENTS, String.class));
    }

    @Override
    public ExperimentDto findExperiment(String experimentAccession, String accessKey) {
        return getSingleExperiment(
                jdbcTemplate.query(
                        SELECT_EXPERIMENT_BY_ACCESSION_AND_ACCESS_KEY,
                        new ScxaExperimentDtoResultSetExtractor(),
                        experimentAccession,
                        accessKey),
                experimentAccession);
    }

    @Override
    public ExperimentDto getExperimentAsAdmin(String experimentAccession) {
        return getSingleExperiment(
                jdbcTemplate.query(
                        SELECT_EXPERIMENT_AS_ADMIN_BY_ACCESSION,
                        new ScxaExperimentDtoResultSetExtractor(),
                        experimentAccession),
                experimentAccession);
    }

    @Override
    public List<ExperimentDto> getAllExperimentsAsAdmin() {
        return jdbcTemplate.query(SELECT_ALL_EXPERIMENTS_AS_ADMIN, new ScxaExperimentDtoResultSetExtractor());
    }

    @Override
    public int countExperiments() {
        return jdbcTemplate.queryForObject(COUNT_EXPERIMENTS, Integer.class);
    }

    @Override
    public void setExperimentPrivacyStatus(String experimentAccession, boolean isPrivate) {
        int recordsCount = jdbcTemplate.update(UPDATE_EXPERIMENT, isPrivate, experimentAccession);
        checkExperimentFound(recordsCount == 1, experimentAccession);
    }

    @Override
    public void deleteExperiment(String experimentAccession) {
        int deletedRecordsCount = jdbcTemplate.update(DELETE_EXPERIMENT, experimentAccession);
        checkExperimentFound(deletedRecordsCount == 1, experimentAccession);
    }

    private ExperimentDto getSingleExperiment(List<ExperimentDto> experimentDtos, String accession) {
        checkExperimentFound(experimentDtos.size() == 1, accession);
        return experimentDtos.get(0);
    }

    // Returns comma-separated IDs, or null if there are no IDs
    private String getPublicationIdsAsString(Collection<String> publicationIds) {
        return publicationIds.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.joining(", "),
                        (result) -> result.isEmpty() ? null : result));
    }
}
