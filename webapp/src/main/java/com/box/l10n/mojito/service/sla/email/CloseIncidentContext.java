package com.box.l10n.mojito.service.sla.email;

import com.box.l10n.mojito.mustache.MustacheBaseContext;

/**
 * @author jeanaurambault
 */
public class CloseIncidentContext extends MustacheBaseContext {

  Long incidentId;

  public CloseIncidentContext(Long incidentId) {
    this.incidentId = incidentId;
  }
}
