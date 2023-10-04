package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.model.OntologyTerm;

import java.util.Arrays;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Getter
enum SupportedOrgan {
    PANCREAS("pancreas", "http://purl.obolibrary.org/obo/UBERON_0001264"),
    KIDNEY("kidney", "http://purl.obolibrary.org/obo/UBERON_0002113"),
    LIVER("liver", "http://purl.obolibrary.org/obo/UBERON_0002107"),
    PLACENTA("placenta", "http://purl.obolibrary.org/obo/UBERON_0001987"),
    LUNG("lung", "http://purl.obolibrary.org/obo/UBERON_0002048"),
    INTESTINE("intestine", "http://purl.obolibrary.org/obo/UBERON_0000160");

    final String name;
    final String ontologyUri;
    final String ontologyAccession;

    SupportedOrgan(String name, String ontologyUri) {
        this.name = name;
        this.ontologyUri = ontologyUri;
        this.ontologyAccession = OntologyTerm.create(ontologyUri).accession();
    }

}


@Service
public class OntologyAccessionsSearchService {
    private final OntologyAccessionsSearchDao ontologyAccessionsSearchDao;

    public OntologyAccessionsSearchService(OntologyAccessionsSearchDao ontologyAccessionsSearchDao) {
        this.ontologyAccessionsSearchDao = ontologyAccessionsSearchDao;
    }

    public ImmutableMap<String, ImmutableSet<String>> searchAvailableAnnotationsForOrganAnatomogram(String experimentAccession) {
        return Arrays.stream(SupportedOrgan.values())
                .parallel()
                .collect(toImmutableMap(
                        SupportedOrgan::getName,
                        supportedOrgan ->
                                ontologyAccessionsSearchDao.searchOntologyAnnotations(
                                        experimentAccession, supportedOrgan.getOntologyUri()).stream()
                                        .map(ontologyUri -> OntologyTerm.createFromURI(ontologyUri).accession())
                                        .collect(toImmutableSet())))
                .entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }
}
