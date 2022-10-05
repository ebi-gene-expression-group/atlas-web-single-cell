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
import uk.ac.ebi.atlas.search.geneids.QueryParsingException;
import uk.ac.ebi.atlas.search.organismpart.OrganismPartSearchService;
import uk.ac.ebi.atlas.search.species.SpeciesSearchService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(classes = TestConfig.class)
class JsonGeneSearchControllerIT {
    @Mock
    private GeneIdSearchService geneIdSearchServiceMock;

    @Mock
    private GeneSearchService geneSearchServiceMock;

    @Mock
    private OrganismPartSearchService organismPartSearchServiceMock;

    @Inject
    private ExperimentTrader experimentTrader;

    @Inject
    private ExperimentAttributesService experimentAttributesService;

    @Mock
    private SpeciesSearchService speciesSearchService;

    private JsonGeneSearchController subject;

    @BeforeEach
    void setUp() {
        subject =
                new JsonGeneSearchController(
                        geneIdSearchServiceMock,
                        geneSearchServiceMock,
                        experimentTrader,
                        experimentAttributesService,
                        organismPartSearchServiceMock,
                        speciesSearchService);
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

    @Test
    void whenRequestParamIsEmptyOrganismPartSearchReturnsEmptySet() {
        var requestParams = new LinkedMultiValueMap<String, String>();

        when(geneIdSearchServiceMock.getGeneQueryByRequestParams(requestParams))
                .thenThrow(new QueryParsingException("Error parsing query"));

        assertThatExceptionOfType(QueryParsingException.class)
                .isThrownBy(() -> subject.getOrganismPartBySearchTerm(requestParams));
    }

    @Test
    void whenSearchTermIsNotFoundAnyGeneIdsReturnsEmptySet() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String searchTermNotInDB = randomAlphabetic(1, 12);
        var generalCategory = "q";
        requestParams.add(generalCategory, searchTermNotInDB);

        GeneQuery geneQuery = GeneQuery.create(searchTermNotInDB);

        when(geneIdSearchServiceMock.getGeneQueryByRequestParams(requestParams))
                .thenReturn(geneQuery);
        when(geneIdSearchServiceMock.search(geneQuery))
                .thenReturn(Optional.of(ImmutableSet.of()));
        when(organismPartSearchServiceMock.search(ImmutableSet.of()))
                .thenReturn(ImmutableSet.of());

        var emptyOrganismPartSet = subject.getOrganismPartBySearchTerm(requestParams);

        assertThat(emptyOrganismPartSet).isEmpty();
    }

    @Test
    void whenSearchTermIsFoundButNoRelatedCellIdsThenReturnsEmptySet() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String searchTermInDB = randomAlphabetic(1, 12);
        var generalCategory = "q";
        requestParams.add(generalCategory, searchTermInDB);
        var geneIdsFromService = ImmutableSet.of(searchTermInDB);

        GeneQuery geneQuery = GeneQuery.create(searchTermInDB);

        when(geneIdSearchServiceMock.getGeneQueryByRequestParams(requestParams))
                .thenReturn(geneQuery);
        when(geneIdSearchServiceMock.search(geneQuery))
                .thenReturn(Optional.of(geneIdsFromService));
        when(organismPartSearchServiceMock.search(geneIdsFromService))
                .thenReturn(ImmutableSet.of());

        var emptyOrganismPartSet = subject.getOrganismPartBySearchTerm(requestParams);

        assertThat(emptyOrganismPartSet).isEmpty();
    }

    @Test
    void whenSearchTermIsFoundAndThereAreRelatedCellIdsThenReturnsOrganismParts() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String searchTermInDB = randomAlphabetic(1, 12);
        var generalCategory = "q";
        requestParams.add(generalCategory, searchTermInDB);
        var geneIdsFromService = ImmutableSet.of(searchTermInDB);
        var expectedOrganismPart = "primary visual cortex";

        GeneQuery geneQuery = GeneQuery.create(searchTermInDB);

        when(geneIdSearchServiceMock.getGeneQueryByRequestParams(requestParams))
                .thenReturn(geneQuery);
        when(geneIdSearchServiceMock.search(geneQuery))
                .thenReturn(Optional.of(geneIdsFromService));
        when(organismPartSearchServiceMock.search(geneIdsFromService))
                .thenReturn(ImmutableSet.of(expectedOrganismPart));

        var actualOrganismParts = subject.getOrganismPartBySearchTerm(requestParams);

        assertThat(actualOrganismParts).containsExactly(expectedOrganismPart);
    }

    @Test
    void whenRequestParamIsEmptySpeciesSearchReturnsAnException() {
        var requestParams = new LinkedMultiValueMap<String, String>();

        when(geneIdSearchServiceMock.getCategoryFromRequestParams(requestParams))
                .thenThrow(new QueryParsingException("Error parsing query"));

        assertThatExceptionOfType(QueryParsingException.class)
                .isThrownBy(() -> subject.getSpeciesByGeneId(requestParams));
    }

    @Test
    void whenRequestParamIsNullSpeciesSearchReturnsAnException() {
        LinkedMultiValueMap<String, String> requestParams = null;

        when(geneIdSearchServiceMock.getCategoryFromRequestParams(requestParams))
                .thenThrow(new QueryParsingException("Error parsing query"));

        assertThatExceptionOfType(QueryParsingException.class)
                .isThrownBy(() -> subject.getSpeciesByGeneId(requestParams));
    }

    @Test
    void whenGeneIdIsNotPartOfAnyExperimentThenReturnsEmptySetOfSpecies() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        var notPartOfAnyExperiment = "NOTPartOfAnyExperiment";
        var generalCategory = "q";
        requestParams.add(generalCategory, notPartOfAnyExperiment);

        when(geneIdSearchServiceMock.getCategoryFromRequestParams(requestParams))
                .thenReturn(generalCategory);
        when(geneIdSearchServiceMock.getFirstNotBlankQueryField(List.of(notPartOfAnyExperiment)))
                .thenReturn(Optional.of(notPartOfAnyExperiment));
        when(speciesSearchService.search(notPartOfAnyExperiment, generalCategory))
                .thenReturn(ImmutableSet.of());

        var emptySpeciesResult = subject.getSpeciesByGeneId(requestParams);

        assertThat(emptySpeciesResult).isEmpty();
    }

    @Test
    void whenGeneIdIsPArtOfSomeExperimentsThenReturnsSetOfSpecies() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        var mostInterestingGeneEver = "MostInterestingGeneEver";
        var generalCategory = "q";
        var expectedSpecies = ImmutableSet.of("Homo_sapiens", "Mus_musculus");
        requestParams.add(generalCategory, mostInterestingGeneEver);

        when(geneIdSearchServiceMock.getCategoryFromRequestParams(requestParams))
                .thenReturn(generalCategory);
        when(geneIdSearchServiceMock.getFirstNotBlankQueryField(List.of(mostInterestingGeneEver)))
                .thenReturn(Optional.of(mostInterestingGeneEver));
        when(speciesSearchService.search(mostInterestingGeneEver, generalCategory))
                .thenReturn(expectedSpecies);

        var speciesResultByGeneId = subject.getSpeciesByGeneId(requestParams);

        assertThat(speciesResultByGeneId).hasSize(2);
        assertThat(speciesResultByGeneId).containsSequence(expectedSpecies);
    }
}