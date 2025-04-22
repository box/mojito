package com.box.l10n.mojito.service.evolve;

import com.box.l10n.mojito.service.evolve.dto.TokenResponseDTO;
import com.google.common.base.Preconditions;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty("l10n.evolve.url")
public class EvolveOAuthClient {
  private final EvolveConfigurationProperties evolveConfigurationProperties;

  private TokenResponseDTO tokenResponse;

  @Autowired
  EvolveOAuthClient(EvolveConfigurationProperties evolveConfigurationProperties) {
    this.evolveConfigurationProperties = evolveConfigurationProperties;
  }

  private PrivateKey privateKey;

  @PostConstruct
  private void init() throws IOException {
    PEMParser pemParser =
        new PEMParser(new StringReader(this.evolveConfigurationProperties.getPrivateKey()));
    Object object = pemParser.readObject();
    pemParser.close();

    PrivateKeyInfo privateKeyInfo;
    if (object instanceof PEMKeyPair pemKeyPair) {
      privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
    } else {
      throw new IllegalArgumentException("Invalid private key format");
    }

    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
    privateKey = converter.getPrivateKey(privateKeyInfo);
  }

  private String generateJwt() {
    long nowMillis = System.currentTimeMillis();
    Date now = new Date(nowMillis);
    Date exp = new Date(nowMillis + 60000); // 1 minute expiration

    return Jwts.builder()
        .setIssuer(this.evolveConfigurationProperties.getApiUid())
        .setAudience(this.evolveConfigurationProperties.getUrl())
        .claim("scope", "admin_read admin_write")
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(privateKey, SignatureAlgorithm.RS256)
        .compact();
  }

  private void refreshAccessToken() {
    RestTemplate restTemplate = new RestTemplate();
    String jwtToken = generateJwt();

    Map<String, String> request = new HashMap<>();
    request.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
    request.put("assertion", jwtToken);

    this.tokenResponse =
        restTemplate.postForObject(
            this.evolveConfigurationProperties.getUrl() + "/oauth2/token.json",
            request,
            TokenResponseDTO.class);
  }

  private boolean isTokenExpired() {
    Preconditions.checkNotNull(this.tokenResponse);
    return ZonedDateTime.now()
        .isAfter(this.tokenResponse.getCreatedAt().plusSeconds(this.tokenResponse.getExpiresIn()));
  }

  synchronized String getAccessToken() {
    if (this.tokenResponse == null || this.isTokenExpired()) {
      this.refreshAccessToken();
    }
    return this.tokenResponse.getAccessToken();
  }
}
