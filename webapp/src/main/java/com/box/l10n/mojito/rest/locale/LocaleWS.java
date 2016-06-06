package com.box.l10n.mojito.rest.locale;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import static com.box.l10n.mojito.rest.locale.LocaleSpecification.bcp47TagEquals;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.springframework.data.jpa.domain.Specifications.where;

/**
 * @author wyau
 */
@RestController
public class LocaleWS {

    @Autowired
    LocaleRepository localeRepository;

    @RequestMapping(value = "/api/locales", method = RequestMethod.GET)
    public List<Locale> getLocales(@RequestParam(value = "bcp47Tag", required = false) String bcp47Tag) {

        return localeRepository.findAll(
                where(ifParamNotNull(bcp47TagEquals(bcp47Tag)))
        );
    }
}
