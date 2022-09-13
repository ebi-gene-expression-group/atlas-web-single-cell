package uk.ac.ebi.atlas.search.geneids;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.LinkedMultiValueMap;
import uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.species.SpeciesProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.ac.ebi.atlas.search.geneids.GeneIdSearchService.VALID_QUERY_FIELDS;
import static uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName.SYMBOL;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_PROPERTY_NAMES;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.SPECIES_OVERRIDE_PROPERTY_NAMES;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomKnownBioentityPropertyName;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeneIdSearchServiceTest {
    private static final Species HUMAN =
            new Species(
                    "Homo sapiens",
                    SpeciesProperties.create("Homo_sapiens", "ORGANISM_PART", "animals", ImmutableList.of()));

    @Mock
    private GeneIdSearchDao geneIdSearchDaoMock;

    @Mock
    private SpeciesFactory speciesFactory;

    private InOrder inOrder;

    private GeneIdSearchService subject;

    @BeforeEach
    void setUp() {
        subject = new GeneIdSearchService(geneIdSearchDaoMock, speciesFactory);
        inOrder = inOrder(geneIdSearchDaoMock);
    }

    @Test
    void geneQueryWithoutCategoryIsSearchedInIdProperties() {
        subject.search(GeneQuery.create("foobar"));
        subject.search(GeneQuery.create("foobar", HUMAN));

        BIOENTITY_PROPERTY_NAMES.forEach(propertyName ->
                inOrder.verify(geneIdSearchDaoMock).searchGeneIds("foobar", propertyName.name));

        BIOENTITY_PROPERTY_NAMES.forEach(propertyName ->
                inOrder.verify(geneIdSearchDaoMock).searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName()));
    }

    @Test
    void speciesSpecificCategoriesIgnoreSpecies() {
        SPECIES_OVERRIDE_PROPERTY_NAMES.forEach(propertyName -> {
            subject.search(GeneQuery.create("foobar", propertyName, HUMAN));
            inOrder.verify(geneIdSearchDaoMock).searchGeneIds("foobar", propertyName.name);
        });

        SPECIES_OVERRIDE_PROPERTY_NAMES.forEach(propertyName ->
            verify(geneIdSearchDaoMock, never()).searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName()));
    }

    @Test
    void multiSpeciesCategoriesHonourSpecies() {
        BioentityPropertyName propertyName = generateRandomKnownBioentityPropertyName();
        while (SPECIES_OVERRIDE_PROPERTY_NAMES.contains(propertyName)) {
            propertyName = generateRandomKnownBioentityPropertyName();
        }

        subject.search(GeneQuery.create("foobar", propertyName, HUMAN));
        verify(geneIdSearchDaoMock).searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName());
    }

    @Test
    void ifNoIdMatchesWeGetEmptyOptional() {
        assertThat(subject.search(GeneQuery.create("foobar", HUMAN)))
                .isEqualTo(subject.search(GeneQuery.create("foobar")))
                .isEmpty();
    }

    @Test
    void ifAtLeastOneIdMatchesWeGetNonEmptyOptional() {
        BioentityPropertyName randomIdPropertyName = generateRandomKnownBioentityPropertyName();
        while (!SPECIES_OVERRIDE_PROPERTY_NAMES.contains(randomIdPropertyName)) {
            randomIdPropertyName = generateRandomKnownBioentityPropertyName();
        }

        BIOENTITY_PROPERTY_NAMES.forEach(propertyName -> {
            when(geneIdSearchDaoMock.searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName()))
                    .thenReturn(Optional.empty());
            when(geneIdSearchDaoMock.searchGeneIds("foobar", propertyName.name))
                    .thenReturn(Optional.empty());
        });

        when(geneIdSearchDaoMock.searchGeneIds("foobar", randomIdPropertyName.name, HUMAN.getEnsemblName()))
                .thenReturn(Optional.of(ImmutableSet.of()));
        when(geneIdSearchDaoMock.searchGeneIds("foobar", randomIdPropertyName.name))
                .thenReturn(Optional.of(ImmutableSet.of()));

        assertThat(subject.search(GeneQuery.create("foobar", HUMAN)))
                .hasValue(ImmutableSet.of());
        assertThat(subject.search(GeneQuery.create("foobar")))
                .hasValue(ImmutableSet.of());
    }

    @Test
    void resultsOfFirstIdThatMatchesAreReturned() {
        BioentityPropertyName randomIdPropertyName = generateRandomKnownBioentityPropertyName();
        while (!BIOENTITY_PROPERTY_NAMES.contains(randomIdPropertyName)) {
            randomIdPropertyName = generateRandomKnownBioentityPropertyName();
        }

        ImmutableList<BioentityPropertyName> idPropertyNamesBefore =
                BIOENTITY_PROPERTY_NAMES.subList(0, BIOENTITY_PROPERTY_NAMES.indexOf(randomIdPropertyName));

        ImmutableList<BioentityPropertyName> idPropertyNamesAfter =
                BIOENTITY_PROPERTY_NAMES.subList(
                        BIOENTITY_PROPERTY_NAMES.indexOf(randomIdPropertyName) + 1, BIOENTITY_PROPERTY_NAMES.size());

        idPropertyNamesBefore.forEach(propertyName -> {
            when(geneIdSearchDaoMock.searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName()))
                    .thenReturn(Optional.of(ImmutableSet.of()));
            when(geneIdSearchDaoMock.searchGeneIds("foobar", propertyName.name))
                    .thenReturn(Optional.of(ImmutableSet.of()));
        });

        idPropertyNamesAfter.forEach(propertyName -> {
            when(geneIdSearchDaoMock.searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName()))
                    .thenReturn(Optional.of(ImmutableSet.of("ENSFOOBAR0000002")));
            when(geneIdSearchDaoMock.searchGeneIds("foobar", propertyName.name))
                    .thenReturn(Optional.of(ImmutableSet.of("ENSFOOBAR0000002")));
        });

        when(geneIdSearchDaoMock.searchGeneIds("foobar", randomIdPropertyName.name, HUMAN.getEnsemblName()))
                .thenReturn(Optional.of(ImmutableSet.of("ENSFOOBAR0000001")));
        when(geneIdSearchDaoMock.searchGeneIds("foobar", randomIdPropertyName.name))
                .thenReturn(Optional.of(ImmutableSet.of("ENSFOOBAR0000001")));

        assertThat(subject.search(GeneQuery.create("foobar", HUMAN)))
                .isEqualTo(subject.search(GeneQuery.create("foobar")))
                .hasValue(ImmutableSet.of("ENSFOOBAR0000001"));

        idPropertyNamesBefore.forEach(propertyName ->
                inOrder.verify(geneIdSearchDaoMock).searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName()));
        inOrder.verify(geneIdSearchDaoMock).searchGeneIds("foobar", randomIdPropertyName.name, HUMAN.getEnsemblName());

        idPropertyNamesBefore.forEach(propertyName ->
                inOrder.verify(geneIdSearchDaoMock).searchGeneIds("foobar", propertyName.name));
        inOrder.verify(geneIdSearchDaoMock).searchGeneIds("foobar", randomIdPropertyName.name);

        idPropertyNamesAfter.forEach(propertyName -> {
            verify(geneIdSearchDaoMock, never()).searchGeneIds("foobar", propertyName.name, HUMAN.getEnsemblName());
            verify(geneIdSearchDaoMock, never()).searchGeneIds("foobar", propertyName.name);
        });
    }

    @Test
    void ifQueryHasEmptySpeciesSearchAllSpecies() {
        var searchString = randomAlphanumeric(3, 20);
        subject.search(GeneQuery.create(searchString));
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "ensgene");
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "symbol");
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "entrezgene");
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "hgnc_symbol");
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "mgi_id");
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "mgi_symbol");
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "flybase_gene_id");
        verify(geneIdSearchDaoMock).searchGeneIds(searchString, "wbpsgene");
    }

    @Test
    void whenEmptyQueryFieldsGivenThenReturnsError() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String emptyValue = "";
        var aRandomCategory = getCategoriesInRandomOrder().get(0);
        requestParams.add(aRandomCategory, emptyValue);

        assertThatExceptionOfType(QueryParsingException.class)
                .isThrownBy(() -> subject.getGeneQueryByRequestParams(requestParams));
    }

    @Test
    void when2DifferentTypeOfEmptyQueryFieldsGivenThenReturnsError() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        var categoriesInRandomOrder = getCategoriesInRandomOrder();
        var aRandomCategory = categoriesInRandomOrder.get(0);
        var anotherRandomCategory = categoriesInRandomOrder.get(1);
        final String emptyValue = "";
        requestParams.add(aRandomCategory, emptyValue);
        requestParams.add(anotherRandomCategory, emptyValue);

        assertThatExceptionOfType(QueryParsingException.class)
                .isThrownBy(() -> subject.getGeneQueryByRequestParams(requestParams));
    }

    @Test
    void when2DifferentTypeOfValidQueryFieldsGivenThenReturnsThe1stOne() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String geneId = randomAlphabetic(1, 12);
        var categoriesInRandomOrder = getCategoriesInRandomOrder();
        var aRandomCategory = categoriesInRandomOrder.get(0);
        var anotherRandomCategory = categoriesInRandomOrder.get(1);
        requestParams.add(aRandomCategory, geneId);
        requestParams.add(anotherRandomCategory, geneId);

        String expectedCategory = aRandomCategory;

        String actualCategory = subject.getCategoryFromRequestParams(requestParams);

        assertThat(actualCategory).isEqualTo(expectedCategory);
    }

    @Test
    void when2DifferentTypeOfQueryFieldsGivenBut1stIsEmptyAnd2ndIsValidThenReturnsTheSecond() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String emptyValue = "";
        final String geneId = randomAlphabetic(1, 12);
        var categoriesInRandomOrder = getCategoriesInRandomOrder();
        var aRandomCategory = categoriesInRandomOrder.get(0);
        var anotherRandomCategory = categoriesInRandomOrder.get(1);
        requestParams.add(aRandomCategory, emptyValue);
        requestParams.add(anotherRandomCategory, geneId);

        String expectedCategory = anotherRandomCategory;

        String actualCategory = subject.getCategoryFromRequestParams(requestParams);

        assertThat(actualCategory).isEqualTo(expectedCategory);
    }

    @Test
    void whenSameTypeOfQueryFieldsGivenTwiceBut1stIsEmptyAnd2ndIsValidThenReturnsTheSecond() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String emptyValue = "";
        final String geneId = randomAlphabetic(1, 12);
        var categoriesInRandomOrder = getCategoriesInRandomOrder();
        var aRandomCategory = categoriesInRandomOrder.get(0);
        requestParams.add(aRandomCategory, emptyValue);
        requestParams.add(aRandomCategory, geneId);

        String expectedCategory = aRandomCategory;

        String actualCategory = subject.getCategoryFromRequestParams(requestParams);

        assertThat(actualCategory).isEqualTo(expectedCategory);
    }

    @Test
    void whenSameTypeOfValidQueryFieldsGivenTwiceThenReturnsTheFirst() {
        var requestParams = new LinkedMultiValueMap<String, String>();
        final String geneId1 = randomAlphabetic(1, 12);
        final String geneId2 = randomAlphabetic(1, 12);
        var categoriesInRandomOrder = getCategoriesInRandomOrder();
        var aRandomCategory = categoriesInRandomOrder.get(0);
        requestParams.add(aRandomCategory, geneId1);
        requestParams.add(aRandomCategory, geneId2);

        String expectedCategory = aRandomCategory;

        String actualCategory = subject.getCategoryFromRequestParams(requestParams);

        assertThat(actualCategory).isEqualTo(expectedCategory);
    }

    @Test
    void whenRequestParamsEmptyThenThrowQueryParsingException() {
        var requestParams = new LinkedMultiValueMap<String, String>();

        assertThatExceptionOfType(QueryParsingException.class)
                .isThrownBy(() -> subject.getGeneQueryByRequestParams(requestParams));
    }

    @Test
    void whenRequestParamsHasGenericQueryFieldThenGotProperGeneQuery() {
        var searchText = randomAlphabetic(1, 12);
        var categoriesInRandomOrder = getCategoriesInRandomOrder();
        var aRandomCategory = categoriesInRandomOrder.get(0);

        var requestParams = getRequestParams(searchText, "", aRandomCategory);

        GeneQuery expectedGeneQuery =
                GeneQuery.create(searchText, BioentityPropertyName.getByName(aRandomCategory));

        GeneQuery actualGeneQuery = subject.getGeneQueryByRequestParams(requestParams);

        assertThat(actualGeneQuery).isEqualTo(expectedGeneQuery);
    }

    @Test
    void whenRequestParamsHasValidQueryFieldThenGotProperGeneQuery() {
        final Species randomSpecies = generateRandomSpecies();
        var searchText = randomAlphabetic(1, 12);
        final String speciesText = randomSpecies.getName();
        var categoriesInRandomOrder = getCategoriesInRandomOrder();
        var aRandomCategory = categoriesInRandomOrder.get(0);

        var requestParams =
                getRequestParams(searchText, speciesText, aRandomCategory);

        GeneQuery expectedGeneQuery = GeneQuery.create(
                searchText, BioentityPropertyName.getByName(aRandomCategory), randomSpecies);

        when(speciesFactory.create(speciesText)).thenReturn(randomSpecies);

        GeneQuery actualGeneQuery = subject.getGeneQueryByRequestParams(requestParams);

        assertThat(actualGeneQuery).isEqualTo(expectedGeneQuery);
    }

    private static LinkedMultiValueMap<String, String> getRequestParams(
            String symbol, String species, String symbolRequestParam) {
        var requestParams = new LinkedMultiValueMap<String, String>();
        requestParams.add(symbolRequestParam, symbol);
        requestParams.add("species", species);
        return requestParams;
    }

    private List<String> getCategoriesInRandomOrder() {
        var queryFields = new ArrayList<>(VALID_QUERY_FIELDS.asList());
        Collections.shuffle(queryFields);

        return queryFields;
    }
}
