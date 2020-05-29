package uk.ac.ebi.atlas.experiments;

import java.util.List;

public class ScxaExperimentCollections implements ExperimentCollectionsRepository {
    private final ExperimentCollectionDao experimentCollectionDao;

    public ScxaExperimentCollections(ExperimentCollectionDao experimentCollectionDao) {
        this.experimentCollectionDao = experimentCollectionDao;
    }

    @Override
    public List<String> getExperimentCollections(String experimentAccession) {
        return experimentCollectionDao.getExperimentCollection(experimentAccession);
    }
}