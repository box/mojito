package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.ThirdPartySyncAction;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartySyncJobInputTest {

    @Test
    public void testBackwardsCompatibility() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // The old representation of the ThirdPartySyncJobInput class
        Long repositoryId = 120L;
        String thirdPartyProjectId = "0p9o8i7u";
        String localeMapping = "tr:tr-TR";
        String pluralSeparator = " _";
        List<ThirdPartySyncAction> actions = Arrays.asList(ThirdPartySyncAction.MAP_TEXTUNIT, ThirdPartySyncAction.PULL);
        List<String> options = Arrays.asList(
                "smartling-placeholder=CUSTOM",
                "smartling-placeholder-custom=\\{\\{\\}\\}|\\{\\{?.+?\\}\\}?|\\%\\%\\(.+?\\)s|\\%\\(.+?\\)s|\\%\\(.+?\\)d|\\%\\%s|\\%s");

        TPSyncJobInput oldInput = new TPSyncJobInput();
        oldInput.setRepositoryId(repositoryId);
        oldInput.setThirdPartyProjectId(thirdPartyProjectId);
        oldInput.setActions(actions);
        oldInput.setPluralSeparator(pluralSeparator);
        oldInput.setLocaleMapping(localeMapping);
        oldInput.setOptions(options);

        String oldRes = objectMapper.writeValueAsStringUnchecked(oldInput);

        ThirdPartySyncJobInput input = objectMapper.readValueUnchecked(oldRes, ThirdPartySyncJobInput.class);
        assertThat(input).isNotNull();
        assertThat(input.getRepositoryId()).isEqualTo(repositoryId);
        assertThat(input.getThirdPartyProjectId()).isEqualTo(thirdPartyProjectId);
        assertThat(input.getActions()).containsExactlyInAnyOrderElementsOf(actions);
        assertThat(input.getPluralSeparator()).isEqualTo(pluralSeparator);
        assertThat(input.getLocaleMapping()).isEqualTo(localeMapping);
        assertThat(input.getOptions()).containsExactlyInAnyOrderElementsOf(options);
        assertThat(input.getSkipTextUnitsWithPattern()).isNull();
        assertThat(input.getSkipAssetsWithPathPattern()).isNull();
    }

    /**
     * Copy of the signature of the {@link ThirdPartySyncJobInput} class before we introduced new parameters to it
     */
    private static class TPSyncJobInput {

        Long repositoryId;
        String thirdPartyProjectId;
        List<ThirdPartySyncAction> actions;
        String pluralSeparator;
        String localeMapping;
        List<String> options;

        public Long getRepositoryId() {
            return repositoryId;
        }

        public void setRepositoryId(Long repositoryId) {
            this.repositoryId = repositoryId;
        }

        public String getThirdPartyProjectId() {
            return thirdPartyProjectId;
        }

        public void setThirdPartyProjectId(String thirdPartyProjectId) {
            this.thirdPartyProjectId = thirdPartyProjectId;
        }

        public List<ThirdPartySyncAction> getActions() {
            return actions;
        }

        public void setActions(List<ThirdPartySyncAction> actions) {
            this.actions = actions;
        }

        public String getPluralSeparator() {
            return pluralSeparator;
        }

        public void setPluralSeparator(String pluralSeparator) {
            this.pluralSeparator = pluralSeparator;
        }

        public String getLocaleMapping() {
            return localeMapping;
        }

        public void setLocaleMapping(String localeMapping) {
            this.localeMapping = localeMapping;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }
    }
}
