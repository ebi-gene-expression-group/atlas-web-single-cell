package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import java.util.HashMap;

@Component
@Transactional(transactionManager = "txManager", readOnly = true)
public class ScxaSpeciesSummaryDao extends SpeciesSummaryDao {
    public ScxaSpeciesSummaryDao(JdbcTemplate jdbcTemplate, SpeciesFactory speciesFactory) {
        super(jdbcTemplate, speciesFactory);
    }

    @Override
    public ImmutableList<SpeciesSummary> getExperimentCountBySpecies() {
        return jdbcTemplate.query(
                "SELECT species, COUNT(species) AS count FROM experiment WHERE private=FALSE GROUP BY species",
                (resultSet) -> {
                    var result = new HashMap<String, Long>();
                    while (resultSet.next()) {
                        result.merge(
                                speciesFactory.create(resultSet.getString("species")).getReferenceName(),
                                resultSet.getLong("count"),
                                Long::sum);
                    }

                    return result.entrySet().stream()
                            .map(entry -> {
                                var species = speciesFactory.create(entry.getKey());
                                return SpeciesSummary.create(
                                        species.getReferenceName(),
                                        species.getKingdom(),
                                        entry.getValue());
                            })
                            .collect(ImmutableList.toImmutableList());
                });
    }
}
