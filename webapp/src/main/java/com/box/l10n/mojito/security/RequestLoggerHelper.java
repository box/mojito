package com.box.l10n.mojito.security;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

/***
 * Adapted from AbstractRequestLoggingFilter
 */
@Component
@ConditionalOnProperty(value = "l10n.logging.requests.enabled", havingValue = "true")
public class RequestLoggerHelper {

  private static final Set<String> FILTERED_HEADER_NAMES =
      Set.of(
          "Accept",
          "Authorization",
          "Connection",
          "Cookie",
          "From",
          "Host",
          "Origin",
          "Priority",
          "Range",
          "Referer",
          "Upgrade");

  public String summarizeRequest(HttpServletRequest request, RequestLoggingConfig config) {
    StringBuilder msg = new StringBuilder();
    msg.append(config.getBeforeMessagePrefix());

    msg.append("Method=").append(request.getMethod()).append(" ");
    msg.append("RequestURI=").append(request.getRequestURI()).append(" ");

    if (config.includesQueryString()) {
      msg.append("Query=").append(request.getQueryString()).append(" ");
    }

    if (config.includesPayload()) {
      String payload = getPayload(request, config);
      msg.append("Payload=").append(payload).append(" ");
    }

    msg.append(config.getAfterMessagePrefix());
    return msg.toString();
  }

  private String getPayload(HttpServletRequest request, RequestLoggingConfig config) {
    StringBuilder msg = new StringBuilder();
    msg.append(config.getBeforeMessagePrefix());
    msg.append(request.getMethod()).append(' ');
    msg.append(request.getRequestURI());

    if (config.includesQueryString()) {
      String queryString = request.getQueryString();
      if (queryString != null) {
        msg.append('?').append(queryString);
      }
    }

    if (config.includesHeader()) {
      HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
      Enumeration<String> names = request.getHeaderNames();
      while (names.hasMoreElements()) {
        String header = names.nextElement();
        if (isMaskedHeader(header)) {
          headers.set(header, "masked");
        }
      }
      msg.append(", headers=").append(headers);
    }

    if (config.includesPayload()) {
      String payload = getMessagePayload(request, config.getMaxPayloadLength());
      if (payload != null) {
        msg.append(", payload=").append(payload);
      }
    }

    return msg.toString();
  }

  private boolean isMaskedHeader(String name) {
    return FILTERED_HEADER_NAMES.contains(name);
  }

  private String getMessagePayload(HttpServletRequest request, int maxPayloadLength) {
    ContentCachingRequestWrapper wrapper =
        WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
    if (wrapper != null) {
      byte[] buf = wrapper.getContentAsByteArray();
      if (buf.length > 0) {
        int length = Math.min(buf.length, maxPayloadLength);
        try {
          return new String(buf, 0, length, wrapper.getCharacterEncoding());
        } catch (UnsupportedEncodingException ex) {
          return "[unknown]";
        }
      }
    }
    return null;
  }
}
