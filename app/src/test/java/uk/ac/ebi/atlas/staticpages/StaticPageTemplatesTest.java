package uk.ac.ebi.atlas.staticpages;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StaticPageTemplatesTest {
    private static final String PRIVACY_NOTICE_URL =
            "https://www.ebi.ac.uk/data-protection/privacy-notice/embl-ebi-public-website/";

    @Test
    void privacyPolicyLinkIsInAboutTemplateAndNotInHelpTemplate() throws IOException {
        var helpTemplate = readTemplate("templates/thymeleaf/views/help/help_content.html");
        var aboutTemplate = readTemplate("templates/thymeleaf/views/about/about_content.html");

        assertThat(helpTemplate).doesNotContain(PRIVACY_NOTICE_URL);
        assertThat(helpTemplate).doesNotContain("Privacy Policy");

        assertThat(aboutTemplate).contains(PRIVACY_NOTICE_URL);
        assertThat(aboutTemplate).contains("Privacy Policy");
    }

    @Test
    void localNavigationContainsAboutTab() throws IOException {
        var fragmentsTemplate = readTemplate("templates/thymeleaf/fragments/fragments.html");

        assertThat(fragmentsTemplate).contains("id=\"local-nav-about\"");
        assertThat(fragmentsTemplate).contains("th:href=\"@{/about.html}\"");
    }

    private static String readTemplate(String path) throws IOException {
        try (var inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
