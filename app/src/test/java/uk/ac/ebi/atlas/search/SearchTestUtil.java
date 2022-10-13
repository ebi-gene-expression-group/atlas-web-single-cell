package uk.ac.ebi.atlas.search;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.LinkedMultiValueMap;

public class SearchTestUtil {

    @NotNull
    public static LinkedMultiValueMap<String, String> getRequestParams(
            String symbol, String species, String symbolRequestParam) {
        var requestParams = new LinkedMultiValueMap<String, String>();
        requestParams.add(symbolRequestParam, symbol);
        requestParams.add("species", species);
        return requestParams;
    }
}
