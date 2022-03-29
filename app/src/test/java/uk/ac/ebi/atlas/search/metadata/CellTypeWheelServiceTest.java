package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CellTypeWheelServiceTest {
    @Mock
    private CellTypeWheelDao cellTypeWheelDaoMock;

    private CellTypeWheelService subject;

    @BeforeEach
    void setUp() {
        subject = new CellTypeWheelService(cellTypeWheelDaoMock);
    }

    @Test
    void returnsCellTypeWheelValuesWithExperimentAccessions() {
        var experimentAccession = generateRandomExperimentAccession();

        var leukocyteWheel =
                ImmutableList.of(
                        ImmutableList.of(
                            "Homo sapiens",
                            "bone marrow",
                            "native thymus-derived CD4-positive, alpha-beta T cell",
                            experimentAccession
                        ),
                        ImmutableList.of(
                            "Homo sapiens",
                            "bone marrow",
                            "helper T cell",
                            experimentAccession
                        ),
                        ImmutableList.of(
                            "Homo sapiens",
                            "bone marrow",
                            "native B cell",
                            experimentAccession
                        )
                );

        when(cellTypeWheelDaoMock.facetSearchCtwFields("leukocyte")).thenReturn(leukocyteWheel);

        var result = subject.search("leukocyte");

        assertThat(result)
                .containsExactlyInAnyOrder(
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens"),
                                experimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow"),
                                experimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow",
                                        "native thymus-derived CD4-positive, alpha-beta T cell"),
                                experimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow",
                                        "helper T cell"),
                                experimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow",
                                        "native B cell"),
                                experimentAccession));
    }
}
