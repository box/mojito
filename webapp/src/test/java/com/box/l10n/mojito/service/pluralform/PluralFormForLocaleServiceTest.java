package com.box.l10n.mojito.service.pluralform;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralFormForLocale;
import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Not a test but a set of tools to generate locale and plural forms from CLDR that are not yet in Mojito
 *
 * @author jeanaurambault
 */
public class PluralFormForLocaleServiceTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(PluralFormForLocaleServiceTest.class);

    @Autowired
    PluralFormForLocaleService pluralFormForLocaleService;

    @Autowired
    PluralFormForLocaleRepository pluralFormForLocaleRepository;

    @Autowired
    LocaleRepository localeRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Ignore("this is used to generate SQL script, it is not an actual test")
    @Test
    public void generate() throws IOException{
        generateNewLocaleFromCLDR();
        generateNewPluralFormsForExistingLocales();
    }

    public void generateNewLocaleFromCLDR() throws IOException {
        JsonNode languagesEn001 = objectMapper.readTree(new File("node_modules/cldr-data/main/en-001/languages.json"));
        JsonNode availableLocales = objectMapper.readTree(new File("node_modules/cldr-data/availableLocales.json"));


        List<Locale> currentMojitoLocales = localeRepository.findAll();
        List<String> currentMojitoBcp47 = currentMojitoLocales.stream().map(l -> l.getBcp47Tag()).collect(Collectors.toList());

        logger.debug("current mojito bcp47: {}", currentMojitoBcp47);

        StringBuilder sb = new StringBuilder();


        LinkedHashSet<String> cldrAll = new LinkedHashSet<>();

        // Available locale in CLDR
        ((ArrayNode) availableLocales.get("availableLocales")).forEach(l -> cldrAll.add(l.textValue()));

        // Plus all other languages for which there is a display name in CLDR
        languagesEn001.get("main").get("en-001").get("localeDisplayNames").get("languages").fieldNames()
                .forEachRemaining(f -> cldrAll.add(f));

        cldrAll.stream().forEach(l -> {
            if (currentMojitoBcp47.contains(l)) {
                logger.debug("{} exists", l);
            } else {
                Locale locale = new Locale();
                locale.setBcp47Tag(l);
                Locale saved = localeRepository.save(locale);
                sb.append("insert into locale (id, bcp47_tag) values (")
                        .append(saved.getId())
                        .append(", '").append(saved.getBcp47Tag()).append("');\n");
            }
        });

        logger.info(sb.toString());
        Files.write(Paths.get("target/test-output/locale.sql"), sb.toString());
    }

    public void generateNewPluralFormsForExistingLocales() {

        StringBuilder sb = new StringBuilder();

        List<PluralFormForLocale> pluralFormsForLocales = pluralFormForLocaleService.getPluralFormsForLocales();

        for (PluralFormForLocale pluralFormsForLocale : pluralFormsForLocales) {
            PluralFormForLocale findByLocale = pluralFormForLocaleRepository.findByLocaleAndPluralForm(pluralFormsForLocale.getLocale(), pluralFormsForLocale.getPluralForm());

            if (findByLocale == null) {
                PluralFormForLocale savedPluralFormForLocale = pluralFormForLocaleRepository.save(pluralFormsForLocale);

                sb.append("insert into plural_form_for_locale (locale_id, plural_form_id) values (").
                        append(savedPluralFormForLocale.getLocale().getId()).
                        append(",").
                        append(savedPluralFormForLocale.getPluralForm().getId()).
                        append(");\n");

                logger.info("insert into plural_form_for_locale (locale_id, plural_form_id) values ({}, {});",
                        savedPluralFormForLocale.getLocale().getId(),
                        savedPluralFormForLocale.getPluralForm().getId());
            }
        }

        logger.info(sb.toString());
        Files.write(Paths.get("target/test-output/pluralform.sql"), sb.toString());
    }

}
