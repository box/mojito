package com.box.l10n.mojito.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configures insecure access to Kubernetes probes.
 *
 * @author wadimw
 */
@Configuration
@ConfigurationProperties("l10n.actuator.health")
public class ActuatorHealthConfig {

  boolean allowInsecureKubernetesProbes = false;

  public boolean getAllowInsecureKubernetesProbes() {
    return allowInsecureKubernetesProbes;
  }

  public void setAllowInsecureKubernetesProbes(boolean allowInsecureKubernetesProbes) {
    this.allowInsecureKubernetesProbes = allowInsecureKubernetesProbes;
  }
}
