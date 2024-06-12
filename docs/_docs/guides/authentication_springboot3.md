---
layout: doc
title:  "Authentication (Spring Boot 3 on master)"
categories: guides
permalink: /docs/guides/authentication-springboot2/
---

{{ site.mojito_green }}'s default setup comes with a `form login` authentication backed by the database.

    l10n.security.authenticationType=DATABASE
    
Other types of authentication can be used in conjunction with the default `form login`. This enables to have a dual 
authentication scheme (potentially `OAuth` for regular users and `form login` to support tools and API integrations 
like the `CLI`.

Change or add an authentication mechanisms by updating the configuration. Eg. to add OAuth2 append it to the end

    l10n.security.authenticationType=DATABASE,OAUTH2
        
You can chosse to either show the {{ site.mojito_green }}'s login page or to automatically redirect to another page.
Eg. to redirect to Github OAuth when the not authenticated
    
    l10n.security.unauth-redirect-to==/login/oauth2/authorization/github

If the redirect is enabled, it is still possible to access {{ site.mojito_green }}'s login page.
               
### OAuth 2

{{ site.mojito_green }} support `OAuth 2` using standard
[Spring 2 / Spring Security configuration](https://docs.spring.io/spring-security/site/docs/5.3.2.RELEASE/reference/html5/#oauth2login-sample-boot)
 with a few additional {{ site.mojito_green }} settings to customize the UI and how the user name is extracted from the user information payload. 

#### Example with GitHub

Create a `GitHub OAuth app` with `Authorization callback URL`: `http://localhost:8080/login/oauth2/code/github`.
 This URI maps to the `redirect_uri` in OAuth and to `preEstablishedRedirectUri` in Spring settings. 
 The `clientId` and `clientSecret` are available once the app has been created. The homepage URL should not matter
but can be set to: `http://localhost:8080/`

Settings to be added, substituting the client `id` and `secret`:
  
    spring.security.oauth2.client.registration.github.client-id={ACTUAL_VALUE}
    spring.security.oauth2.client.registration.github.client-secret={ACTUAL_VALUE}
    spring.security.oauth2.client.registration.github.provider=github
    l10n.security.oauth2.github.ui-label-text=Github
    l10n.security.oauth2.github.common-name-attribute=name

#### Multiple registrations and providers 

Multiple OAuth registrations and providers can be specified, see 
[Spring security documentation](https://docs.spring.io/spring-security/site/docs/5.3.2.RELEASE/reference/html5/#oauth2login-boot-property-mappings) 
for more details. 

A configuration could look similar to this:

    spring.security.oauth2.client.registration.[registrationId].client-id={ACTUAL_VALUE}
    spring.security.oauth2.client.registration.[registrationId].client-secret={ACTUAL_VALUE}
    spring.security.oauth2.client.registration.[registrationId].scope=user
    spring.security.oauth2.client.registration.[registrationId].redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
    spring.security.oauth2.client.registration.[registrationId].authorization-grant-type=authorization_code
    spring.security.oauth2.client.registration.[registrationId].client-authentication-method=client_secret_post
    spring.security.oauth2.client.registration.[registrationId].client-name={ACTUAL_VALUE} 
    
    spring.security.oauth2.client.provider.[providerId].token-uri=https://authhost.com/oauth/token/
    spring.security.oauth2.client.provider.[providerId].authorization-uri=https://authhost.com/oauth/authorize/
    spring.security.oauth2.client.provider.[providerId].user-info-uri=https://authhost.com/api/user
    spring.security.oauth2.client.provider.[providerId].user-name-attribute=username 
    
    l10n.security.oauth2.[registrationId].ui-label-text=My Personal OAuth

### LDAP

{{ site.mojito_green }} can use `LDAP` for authentication (default uses database) 
by setting the following properties:

    l10n.security.authenticationType=LDAP
    l10n.security.ldap.url=${URL}
    l10n.security.ldap.port=${PORT}
    l10n.security.ldap.root=${ROOT}
    l10n.security.ldap.userSearchBase=${USER_SEARCH_BASE}
    l10n.security.ldap.userSearchFilter=${USER_SEARCH_FILTER}
    l10n.security.ldap.groupSearchBase=${GROUP_SEARCH_BASE}
    l10n.security.ldap.groupSearchFilter=${GROUP_SEARCH_FILTER}
    l10n.security.ldap.groupRoleAttribute=${GROUP_ROLE_ATTR}
    l10n.security.ldap.managerDn=${MANAGER_DN}
    l10n.security.ldap.managerPassword=${MANAGER_PASSWORD}


With `LDAP`, the database is still used to store information about the users
but the server won't contain credentials.
 
When a user logs in via `LDAP` and there is no matching user in {{ site.mojito_green }}, a new
 user is automatically created using the information provided by the `LDAP` server.

The [user management]({{ site.url }}/docs/guides/manage-users) is different
compared to when using the database. The CLI to manage users will only 
change the users in {{ site.mojito_green }} and won't interact with the `LDAP` server. This
means you can't change a user password using this command. 

As of now, there is no real need to manage users with the CLI when using `LDAP`
 as no authorization is implemented yet nor any useful information can be
 added. Later it could be used to change a user role, add a profile pic, etc.
 
### Pre-authenticated with Header 
 
If the authentication is performed by an external system, header pre-authentication can be turn on with following
configuration:

    l10n.security.authenticationType=HEADER,DATABASE
    
 The username is read from the `x-forwarded-user` http header. 
