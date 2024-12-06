package com.box.l10n.mojito.rest.locale;

import static com.box.l10n.mojito.rest.locale.LocaleSpecification.bcp47TagEquals;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.springframework.data.jpa.domain.Specification.where;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wyau
 */
@RestController
public class LocaleWS {

  @Autowired LocaleRepository localeRepository;

  @Operation(summary = "Get Locales for specific BCP 47 Tags")
  @RequestMapping(value = "/api/locales", method = RequestMethod.GET)
  public List<Locale> getLocales(
      @RequestParam(value = "bcp47Tag", required = false) String bcp47Tag) {

    return localeRepository.findAll(where(ifParamNotNull(bcp47TagEquals(bcp47Tag))));
  }
}
