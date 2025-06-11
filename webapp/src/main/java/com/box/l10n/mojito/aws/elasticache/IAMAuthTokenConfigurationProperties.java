package com.box.l10n.mojito.aws.elasticache;

import static software.amazon.awssdk.http.SdkHttpMethod.GET;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.http.SdkHttpMethod;

@Component
@ConfigurationProperties("l10n.redis.iam-auth-token")
public class IAMAuthTokenConfigurationProperties {
  private SdkHttpMethod requestMethod = GET;

  private String requestProtocol = "https://";

  private String actionParameter = "Action";

  private String userParameter = "User";

  private String actionName = "connect";

  private String serviceName = "elasticache";

  private long tokenExpiryDurationSeconds = 900;

  public SdkHttpMethod getRequestMethod() {
    return requestMethod;
  }

  public void setRequestMethod(SdkHttpMethod requestMethod) {
    this.requestMethod = requestMethod;
  }

  public String getRequestProtocol() {
    return requestProtocol;
  }

  public void setRequestProtocol(String requestProtocol) {
    this.requestProtocol = requestProtocol;
  }

  public String getActionParameter() {
    return actionParameter;
  }

  public void setActionParameter(String actionParameter) {
    this.actionParameter = actionParameter;
  }

  public String getUserParameter() {
    return userParameter;
  }

  public void setUserParameter(String userParameter) {
    this.userParameter = userParameter;
  }

  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public long getTokenExpiryDurationSeconds() {
    return tokenExpiryDurationSeconds;
  }

  public void setTokenExpiryDurationSeconds(long tokenExpiryDurationSeconds) {
    this.tokenExpiryDurationSeconds = tokenExpiryDurationSeconds;
  }
}
