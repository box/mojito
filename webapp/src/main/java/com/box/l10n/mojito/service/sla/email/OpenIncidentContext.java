package com.box.l10n.mojito.service.sla.email;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.mustache.MustacheBaseContext;
import java.util.List;

/**
 * @author jeanaurambault
 */
public class OpenIncidentContext extends MustacheBaseContext {

  Long incidentId;
  List<Repository> repositories;
  String mojitoUrl;

  public OpenIncidentContext(Long incidentId, List<Repository> repositories, String mojitoUrl) {
    this.incidentId = incidentId;
    this.repositories = repositories;
    this.mojitoUrl = mojitoUrl;
  }
}
