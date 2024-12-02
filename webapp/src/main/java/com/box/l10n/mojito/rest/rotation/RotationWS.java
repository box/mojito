package com.box.l10n.mojito.rest.rotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jaurambault
 */
@RestController
public class RotationWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RotationWS.class);

  @Autowired HealthRotation healthRotation;

  /**
   * curl http://127.0.0.1:8080/api/rotation -X POST -H "Content-Type: application/json" -d "true"
   */
  @RequestMapping(method = RequestMethod.POST, value = "/api/rotation")
  @ResponseStatus(HttpStatus.OK)
  public void setRotation(@RequestBody Boolean rotation) throws InterruptedException {
    healthRotation.setInRotation(rotation);
  }
}
