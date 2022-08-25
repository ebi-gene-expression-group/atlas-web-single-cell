package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.experimentpage.ExperimentAttributesService;
import uk.ac.ebi.atlas.search.geneids.GeneIdSearchService;
import uk.ac.ebi.atlas.search.geneids.GeneQuery;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(classes = TestConfig.class)
class JsonGeneSearchControllerIT {
    @Mock
    private GeneIdSearchService geneIdSearchServiceMock;

    @Inject
    private SpeciesFactory speciesFactory;

    @Mock
    private GeneSearchService geneSearchServiceMock;

    @Inject
    private ExperimentTrader experimentTrader;

    @Inject
    private ExperimentAttributesService experimentAttributesService;

    private JsonGeneSearchController subject;

    @BeforeEach
    void setUp() {
        subject =
                new JsonGeneSearchController(
                        geneIdSearchServiceMock,
                        speciesFactory,
                        geneSearchServiceMock,
                        experimentTrader,
                        experimentAttributesService);
    }

    @Test
    void ifSpeciesIsNotPresentGeneQueryHasEmptySpeciesField() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String geneId = randomAlphabetic(1, 12);
        requestParams.add("q", geneId);

        GeneQuery geneQuery = GeneQuery.create(geneId);

        when(geneIdSearchServiceMock.getGeneQueryByRequestParams(requestParams))
                .thenReturn(geneQuery);

        subject.search(requestParams);

        var geneQueryArgCaptor = ArgumentCaptor.forClass(GeneQuery.class);
        verify(geneIdSearchServiceMock).search(geneQueryArgCaptor.capture());

        assertThat(geneQueryArgCaptor.getValue().species()).isEmpty();
    }

    @Test
    void whenRequestParamIsEmptyMarkerGeneSearchReturnsFalse() {
        var requestParams = new LinkedMultiValueMap<String, String>();

        boolean isMarkerGene = subject.isMarkerGene(requestParams);

        assertThat(isMarkerGene).isFalse();
    }

    @Test
    void whenRequestParamIsNullMarkerGeneSearchReturnsFalse() {
        LinkedMultiValueMap<String, String> requestParams = null;

        boolean isMarkerGene = subject.isMarkerGene(requestParams);

        assertThat(isMarkerGene).isFalse();
    }

    @Test
    void whenGeneIsNotAMarkerGeneSearchForItReturnsFalse() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String geneId = "NOTMarkerGene";
        requestParams.add("q", geneId);

        GeneQuery geneQuery = GeneQuery.create(geneId);

        when(geneIdSearchServiceMock.getGeneQueryByRequestParams(requestParams))
                .thenReturn(geneQuery);
        when(geneIdSearchServiceMock.search(geneQuery))
                .thenReturn(Optional.empty());

        boolean isMarkerGene = subject.isMarkerGene(requestParams);

        assertThat(isMarkerGene).isFalse();
    }

    @Test
    void whenGeneIsAMarkerGeneSearchForItReturnsTrue() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String geneId = "AT2G23910";
        final String experimentAccession = "E-CURD-4";
        final String cellId = "SRR8206663-CACTCTTATAGG";
        final int kValue = 101;
        final List<Integer> clusterIds = List.of(1, 2, 3, 4);
        requestParams.add("q", geneId);

        GeneQuery geneQuery = GeneQuery.create(geneId);

        when(geneIdSearchServiceMock.getGeneQueryByRequestParams(requestParams))
                .thenReturn(geneQuery);
        when(geneIdSearchServiceMock.search(geneQuery))
                .thenReturn(Optional.of(ImmutableSet.of(geneId)));
        when(geneSearchServiceMock.getCellIdsInExperiments(geneId))
                .thenReturn(Map.of(geneId, Map.of(experimentAccession, List.of(cellId))));
        when(geneSearchServiceMock.getMarkerGeneProfile(geneId))
                .thenReturn(ImmutableMap.of(geneId, Map.of(experimentAccession, Map.of(kValue, clusterIds))));

        boolean isMarkerGene = subject.isMarkerGene(requestParams);

        assertThat(isMarkerGene).isTrue();
    }
}