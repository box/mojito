---
layout: doc
title:  "Spring Boot 3 migration"
categories: guides
permalink: /docs/guides/authentication-springboot3/
---

Changes from Spring 2.x to Spring 3.x

## Spring session

Following property is not supported anymore, and is removed

```properties
spring.session.store-type=none
```

## Statsd property update

```properties
management.metrics.export.statsd.enabled 
```

becomes

```properties
management.statsd.metrics.export.enabled
```

## Oauth2 configuration

```properties
spring.security.oauth2.client.registration.[registrationId].client-authentication-method=post
```

becomes

```properties
spring.security.oauth2.client.registration.[registrationId].client-authentication-method=client_secret_post
```