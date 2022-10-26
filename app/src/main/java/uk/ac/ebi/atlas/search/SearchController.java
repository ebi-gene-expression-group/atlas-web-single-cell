package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_PROPERTY_NAMES;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Controller
public class SearchController extends HtmlExceptionHandlingController {
    private static final ImmutableList<String> VALID_REQUEST_PARAMS =
            ImmutableList.<String>builder()
                    .add("q")
                    .addAll(BIOENTITY_PROPERTY_NAMES.stream().map(BioentityPropertyName::name).collect(toList()))
                    .build();

    private static final String SEARCH_ENDPOINT = "/search";

    @RequestMapping(value = SEARCH_ENDPOINT, method = RequestMethod.POST)
    public String parseJsonAsRequestParamsAndRedirect(
            @RequestParam(value = "geneQuery") String query,
            @RequestParam(value = "species", defaultValue = "") String species) {
        var queryObject = GSON.fromJson(query, JsonObject.class);

        var searchUrlBuilder = UriComponentsBuilder
                .newInstance()
                .path(SEARCH_ENDPOINT);

        if (queryObject.get("category").getAsString().equals("metadata")) {
            searchUrlBuilder
                    .pathSegment("metadata", queryObject.get("term").getAsString());
        } else {
            searchUrlBuilder
                    .queryParam(queryObject.get("category").getAsString(), queryObject.get("term").getAsString())
                    .queryParam("species", species);
        }

        return "redirect:" + searchUrlBuilder.build().toUriString();
    }

    @RequestMapping(value = SEARCH_ENDPOINT, method = RequestMethod.GET, produces = "text/html;charset=UTF-8")
    public String getSearch(@RequestParam MultiValueMap<String, String> requestParams,
                            Model model) {
        var endpoint =
                UriComponentsBuilder
                        .newInstance()
                        .path("json/search")
                        .queryParams(requestParams)
                        .build()
                        .toUriString();

        model.addAttribute("endpoint", endpoint);

        if (requestParams.containsKey("species") && isNotEmpty(requestParams.getFirst("species"))) {
            model.addAttribute("species", requestParams.getFirst("species"));
        }

        for (var requestParam : requestParams.keySet()) {
            for (var idPropertyName : VALID_REQUEST_PARAMS) {
                if (requestParam.equalsIgnoreCase(idPropertyName)) {
                    model.addAttribute("geneQueryTerm", requestParams.getFirst(requestParam));
                    model.addAttribute("geneQueryCategory", requestParam);
                    break;
                }
            }
        }

        return "gene-search-results";
    }
}
