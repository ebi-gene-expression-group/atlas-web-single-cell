package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableMap;
import io.atlassian.util.concurrent.LazyReference;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.text.NumberFormat;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Component
public class LatestExperimentsService {
    private final LatestExperimentsDao latestExperimentsDao;
    private final ExperimentTrader experimentTrader;
    private final LazyReference<ImmutableMap<String, Object>> latestExperimentsAttributes =
        new LazyReference<>() {
            @Override
            protected ImmutableMap<String, Object> create() {
                long experimentCount = latestExperimentsDao.fetchPublicExperimentsCount();

                var latestExperimentInfo =
                        latestExperimentsDao.fetchLatestExperimentAccessions().stream()
                                .map(experimentTrader::getPublicExperiment)
                                .map(ExperimentJsonSerializer::serialize)
                                .collect(toImmutableSet());
                return ImmutableMap.of(
                        "experimentCount", experimentCount,
                        "formattedExperimentCount", NumberFormat.getInstance().format(experimentCount),
                        "latestExperiments", latestExperimentInfo);
            }
        };

    public LatestExperimentsService(LatestExperimentsDao latestExperimentsDao,
                                    ExperimentTrader experimentTrader) {
        this.latestExperimentsDao = latestExperimentsDao;
        this.experimentTrader = experimentTrader;
    }

    public ImmutableMap<String, Object> fetchLatestExperimentsAttributes() {
            return latestExperimentsAttributes.get();
    }
}
