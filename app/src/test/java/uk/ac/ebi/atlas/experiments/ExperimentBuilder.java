package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import uk.ac.ebi.atlas.model.arraydesign.ArrayDesign;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
import uk.ac.ebi.atlas.model.experiment.ExperimentDisplayDefaults;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.experiment.sample.Cell;
import uk.ac.ebi.atlas.model.experiment.sample.ReportsGeneExpression;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.species.Species;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.apache.commons.lang3.RandomStringUtils.*;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.*;

// It’s funny how unnecessary the builder was in main and how much it helps to keep tests DRY. If we bring the builder
// back, I think this design using generics is much better than the previous one.
public abstract class ExperimentBuilder<R extends ReportsGeneExpression, E extends Experiment<R>> {
    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();

    private final int dataProvidersSize = RNG.nextInt(3);
    private final int alternativeViewsSize = RNG.nextInt(4);

    List<String> technologyType = Arrays.asList(randomAlphabetic(6));
    ExperimentType experimentType = getRandomExperimentType();
    String experimentAccession = generateRandomExperimentAccession();
    Collection<String> secondaryExperimentAccessions = RNG.nextBoolean() ?
            ImmutableSet.of(generateRandomPrideExperimentAccession()) : ImmutableSet.of();
    String experimentDescription = randomAlphabetic(60);
    Date loadDate = new Date();
    Date lastUpdate = new Date();
    Species species = generateRandomSpecies();
    ImmutableList<R> samples;
    ExperimentDesign experimentDesign = new ExperimentDesign();
    ImmutableList<String> pubMedIds =
            IntStream.range(0, 5).boxed()
                    .map(__ -> randomNumeric(3, 8))
                    .collect(toImmutableList());
    ImmutableList<String> dois =
            IntStream.range(0, 5).boxed()
                    .map(__ -> "http://dx.doi.org/10." + randomNumeric(4) + "/" + randomAlphanumeric(4,10))
                    .collect(toImmutableList());

    // Only for baseline experiments
    String displayName = randomAlphabetic(10, 40);
    String disclaimer = randomAlphabetic(300);
    ImmutableList<String> dataProviderDescriptions =
            IntStream.range(0, dataProvidersSize).boxed()
                    .map(__ -> randomAlphabetic(40))
                    .collect(toImmutableList());
    ImmutableList<String> dataProviderUrls =
            IntStream.range(0, dataProvidersSize).boxed()
                    .map(__ -> "https://www." + randomAlphabetic(4, 10) + ".org/" + randomAlphabetic(0, 10))
                    .collect(toImmutableList());
    ImmutableList<String> alternativeViews =
            IntStream.range(0, alternativeViewsSize).boxed()
                    .map(__ -> generateRandomExperimentAccession())
                    .collect(toImmutableList());
    ImmutableList<String> alternativeViewDescriptions =
            IntStream.range(0, alternativeViewsSize).boxed()
                    .map(__ -> randomAlphabetic(10, 40))
                    .collect(toImmutableList());
    ExperimentDisplayDefaults experimentDisplayDefaults = ExperimentDisplayDefaults.create();

    // Only for differential experiments
    ImmutableList<Boolean> cttvPrimaryContrastAnnotations;
    // Only for microarray experiments
    ImmutableList<ArrayDesign> arrayDesigns =
            IntStream.range(1, 5).boxed()
                    .map(__ -> generateRandomArrayDesignAccession())
                    .map(ArrayDesign::create)
                    .collect(toImmutableList());

    boolean isPrivate = RNG.nextBoolean();

    String accessKey = UUID.randomUUID().toString();

    private <T> ImmutableList<T> pad(List<T> list, int n, Supplier<T> supplier) {
        if (list.size() >= n) {
            return ImmutableList.copyOf(list.subList(0, n));
        }

        ArrayList<T> workList = Lists.newArrayList(list);
        while (workList.size() < n) {
            workList.add(supplier.get());
        }

        return ImmutableList.copyOf(workList);
    }

    public ExperimentBuilder<R, E> withExperimentType(ExperimentType experimentType) {
        this.experimentType = experimentType;
        return this;
    }

    public ExperimentBuilder<R, E> withExperimentAccession(String experimentAccession) {
        this.experimentAccession = experimentAccession;
        return this;
    }

    public ExperimentBuilder<R, E> withTechnologyType(List<String> technologyTypes) {
        this.technologyType = ImmutableList.copyOf(technologyTypes);
        return this;
    }

    public ExperimentBuilder<R, E> withExperimentDescription(String experimentDescription) {
        this.experimentDescription = experimentDescription;
        return this;
    }

    public ExperimentBuilder<R, E> withLoadDate(Date loadDate) {
        this.loadDate = loadDate;
        return this;
    }

    public ExperimentBuilder<R, E> withLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public ExperimentBuilder<R, E> withSpecies(Species species) {
        this.species = species;
        return this;
    }

    public ExperimentBuilder<R, E> withSamples(List<R> samples) {
        this.samples = ImmutableList.copyOf(samples);
        return this;
    }

    public ExperimentBuilder<R, E> withExperimentDesign(ExperimentDesign experimentDesign) {
        this.experimentDesign= experimentDesign;
        return this;
    }

    public ExperimentBuilder<R, E> withPubMedIds(Collection<String> pubMedIds) {
        this.pubMedIds= ImmutableList.copyOf(pubMedIds);
        return this;
    }

    public ExperimentBuilder<R, E> withDois(Collection<String> dois) {
        this.dois= ImmutableList.copyOf(dois);
        return this;
    }

    public ExperimentBuilder<R, E> withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ExperimentBuilder<R, E> withDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
        return this;
    }

    public ExperimentBuilder<R, E> withDataProviderUrls(Collection<String> dataProviderUrls) {
        this.dataProviderUrls= ImmutableList.copyOf(dataProviderUrls);
        return this;
    }

    public ExperimentBuilder<R, E> withDataProviderDescriptions(Collection<String> dataProviderDescriptions) {
        this.dataProviderDescriptions= ImmutableList.copyOf(dataProviderDescriptions);
        return this;
    }

    public ExperimentBuilder<R, E> withAlternativeViews(Collection<String> alternativeViews) {
        this.alternativeViews = ImmutableList.copyOf(alternativeViews);
        return this;
    }

    public ExperimentBuilder<R, E> withAlternativeViewDescriptions(Collection<String> alternativeViewDescriptions) {
        this.alternativeViewDescriptions = ImmutableList.copyOf(alternativeViewDescriptions);
        return this;
    }

    public ExperimentBuilder<R, E> withExperimentDisplayDefaults(ExperimentDisplayDefaults experimentDisplayDefaults) {
        this.experimentDisplayDefaults = experimentDisplayDefaults;
        return this;
    }

    public ExperimentBuilder<R, E> withCttvAnnotations(List<Boolean> isCttvPrimary) {
        this.cttvPrimaryContrastAnnotations = pad(isCttvPrimary, samples.size(), RNG::nextBoolean);
        return this;
    }

    public ExperimentBuilder<R, E> withArrayDesigns(List<ArrayDesign> arrayDesigns) {
        this.arrayDesigns = ImmutableList.copyOf(arrayDesigns);
        return this;
    }

    public ExperimentBuilder<R, E> withPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        return this;
    }

    public ExperimentBuilder<R, E> withAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public abstract E build();

    private static ExperimentType getRandomExperimentType() {
        return ExperimentType.values()[RNG.nextInt(ExperimentType.values().length)];
    }

    public static class SingleCellBaselineExperimentBuilder extends ExperimentBuilder<Cell,
                                                                                      SingleCellBaselineExperiment> {
        public SingleCellBaselineExperimentBuilder() {
            experimentType = SINGLE_CELL_RNASEQ_MRNA_BASELINE;
            samples =
                    IntStream.range(1, RNG.nextInt(2, 1000)).boxed()
                            .map(__ -> new Cell(randomAlphanumeric(6, 10)))
                            .collect(toImmutableList());
        }

        @Override
        public SingleCellBaselineExperimentBuilder withDisclaimer(String disclaimer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperimentBuilder withDataProviderUrls(Collection<String> dataProviderUrls) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperimentBuilder withDataProviderDescriptions(Collection<String> dataProviderDescriptions) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperimentBuilder withAlternativeViews(Collection<String> alternativeViews) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperimentBuilder withAlternativeViewDescriptions(Collection<String> alternativeViewDescriptions) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperimentBuilder withExperimentDisplayDefaults(ExperimentDisplayDefaults experimentDisplayDefaults) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperimentBuilder withCttvAnnotations(List<Boolean> isCttvPrimary ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperimentBuilder withArrayDesigns(List<ArrayDesign> arrayDesigns) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SingleCellBaselineExperiment build() {
            return new SingleCellBaselineExperiment(
                    experimentType,
                    experimentAccession,
                    secondaryExperimentAccessions,
                    experimentDescription,
                    loadDate,
                    lastUpdate,
                    species,
                    technologyType,
                    samples,
                    experimentDesign,
                    pubMedIds,
                    dois,
                    displayName,
                    isPrivate,
                    accessKey);
        }

        private ImmutableSet<String> getExperimentalFactorHeaders(ExperimentDesign experimentDesign) {
            return ImmutableSet.copyOf(experimentDesign.getFactorHeaders());
        }

    }
}
