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

import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;
import static uk.ac.ebi.atlas.search.SearchTestUtil.getRequestParams;
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
    void whenRequestParamsEmptyThenThrowQueryParsingException() {
        var requestParams = new LinkedMultiValueMap<String, String>();

        assertThatExceptionOfType(QueryParsingException.class)
                .isThrownBy(() -> subject.getGeneQueryByRequestParams(requestParams));
    }

    @Test
    void whenRequestParamsHasGenericQueryFieldThenGotProperGeneQuery() {
        String symbolText = "CFTR";
        LinkedMultiValueMap<String, String> requestParams =
                getRequestParams(symbolText, "", "q");

        GeneQuery expectedGeneQuery = GeneQuery.create(symbolText);

        GeneQuery actualGeneQuery = subject.getGeneQueryByRequestParams(requestParams);

        assertThat(actualGeneQuery).isEqualTo(expectedGeneQuery);
    }

    @Test
    void whenRequestParamsHasValidQueryFieldThenGotProperGeneQuery() {
        final Species randomSpecies = generateRandomSpecies();
        String symbolText = "CFTR";
        final String speciesText = randomSpecies.getName();
        final String symbolRequestParam = "symbol";
        LinkedMultiValueMap<String, String> requestParams =
                getRequestParams(symbolText, speciesText, symbolRequestParam);

        GeneQuery expectedGeneQuery = GeneQuery.create(
                symbolText, BioentityPropertyName.getByName(symbolRequestParam), randomSpecies);

        when(speciesFactory.create(speciesText)).thenReturn(randomSpecies);

        GeneQuery actualGeneQuery = subject.getGeneQueryByRequestParams(requestParams);

        assertThat(actualGeneQuery).isEqualTo(expectedGeneQuery);
    }
}
