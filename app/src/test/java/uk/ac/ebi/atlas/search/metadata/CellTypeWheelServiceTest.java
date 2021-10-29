package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.search.metadata.CellTypeWheelDao;
import uk.ac.ebi.atlas.search.metadata.CellTypeWheelService;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;
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
    void returnsCellTypeWheelValuesPerSearch() {
        String experimentAccession = generateRandomExperimentAccession();

        ImmutableList<ImmutableList<String>> leukocyteWheel =
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

        ImmutableSet<ImmutablePair<ImmutableList<String>, String>> result = subject.search("leukocyte");

        assertThat(result)
                .contains(
                        ImmutablePair.of(
                                ImmutableList.of(experimentAccession),
                                experimentAccession))
                .contains(
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow"),
                                experimentAccession))
                .contains(
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow",
                                        "native thymus-derived CD4-positive, alpha-beta T cell"),
                                experimentAccession))
                .contains(
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow",
                                        "helper T cell"),
                                experimentAccession))
                .contains(
                        ImmutablePair.of(
                                ImmutableList.of(
                                        "Homo sapiens",
                                        "bone marrow",
                                        "native B cell"),
                                experimentAccession));
    }

}
