package uk.ac.ebi.atlas.trader;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrudDao;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.sdrf.SdrfParser;
import uk.ac.ebi.atlas.experiments.ExperimentBuilder;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.trader.factory.SingleCellBaselineExperimentFactory;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomDoi;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomPubmedId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(MockitoExtension.class)
class ScxaExperimentRepositoryTest {
    private static final Random RNG = ThreadLocalRandom.current();
    @Mock
    private ExperimentCrudDao experimentCrudDaoMock;

    @Mock
    private ExperimentDesignParser experimentDesignParserMock;

    @Mock
    private IdfParser idfParserMock;

    @Mock
    private SdrfParser sdrfParserMock;

    @Mock
    private SingleCellBaselineExperimentFactory experimentFactoryMock;

    private ScxaExperimentRepository subject;

    @BeforeEach
    void setUp() {
        subject =
                new ScxaExperimentRepository(
                        experimentCrudDaoMock,
                        experimentDesignParserMock,
                        idfParserMock,
                        experimentFactoryMock,
                        sdrfParserMock);
    }

    @ParameterizedTest
    @MethodSource("bulkExperimentTypeProvider")
    void cannotBuildBulkExperiments(ExperimentType experimentType) {
        var experimentAccession = generateRandomExperimentAccession();

        when(experimentCrudDaoMock.readExperiment(experimentAccession))
                .thenReturn(new ExperimentDto(
                        experimentAccession,
                        experimentType,
                        generateRandomSpecies().getName(),
                        ImmutableList.of(generateRandomPubmedId()),
                        ImmutableList.of(generateRandomDoi()),
                        new Timestamp(new Date().getTime()),
                        new Timestamp(new Date().getTime()),
                        RNG.nextBoolean(),
                        UUID.randomUUID().toString()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> subject.getExperiment(experimentAccession));
    }

    @ParameterizedTest
    @MethodSource("singleCellExperimentTypeProvider")
    void shouldBuildOnlySingleCellExperiments(ExperimentType experimentType) {
        var experimentAccession = generateRandomExperimentAccession();

       var testDto = new ExperimentDto(
          experimentAccession,
          experimentType,
          generateRandomSpecies().getName(),
          ImmutableList.of(generateRandomPubmedId()),
          ImmutableList.of(generateRandomDoi()),
          new Timestamp(new Date().getTime()),
          new Timestamp(new Date().getTime()),
          RNG.nextBoolean(),
          UUID.randomUUID().toString());

        when(experimentCrudDaoMock.readExperiment(experimentAccession)).thenReturn(testDto);

        when(experimentFactoryMock.create(testDto,
          experimentDesignParserMock.parse(testDto.getExperimentAccession()),
          idfParserMock.parse(testDto.getExperimentAccession()),
          sdrfParserMock.parseSingleCellTechnologyType(testDto.getExperimentAccession())))
          .thenReturn(new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
            .withExperimentAccession(testDto.getExperimentAccession())
            .build());

        var result = subject.getExperiment(experimentAccession);
        assertThat(result.getType().isSingleCell()).isTrue();
        assertThat(result.getType().name()).startsWith("SINGLE");
    }

    private static Stream<ExperimentType> bulkExperimentTypeProvider() {
        return Stream.of(ExperimentType.values()).filter(type -> !type.isSingleCell());
    }

    private static Stream<ExperimentType> singleCellExperimentTypeProvider() {
        return Stream.of(ExperimentType.values()).filter(type -> type.isSingleCell());
    }
}
