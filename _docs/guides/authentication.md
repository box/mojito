---
layout: doc
title:  "Authentication"
categories: guides
permalink: /docs/guides/authentication/
---

### OAuth 2

{{ site.mojito_green }} can use `OAuth 2` for authentication. It can be used in conjunction with the default `form login`
 authentication. This enables to have a dual authentication scheme (potentially `OAuth` for 
 regular users and `form login` to support tools and API integrations like the `CLI`.
  
The integration resuse Spring Security standard settings, just prefixed with `l10n`. 

#### Example with GitHub

Create a `GitHub OAuth app` with `Authorization callback URL`: `http://localhost:8080/login/oauth`.
 This URI maps to the `redirect_uri` in OAuth and to `preEstablishedRedirectUri` in Spring settings. 
 The `clientId` and `clientSecret` are available once the app has been created.

Settings to be added, substituting the client `id` and `secret`:

    l10n.security.oauth2.enabled=true
    l10n.security.oauth2.client.clientId={ACTUAL_VALUE}
    l10n.security.oauth2.client.clientSecret={ACTUAL_VALUE}
    l10n.security.oauth2.client.accessTokenUri=https://github.com/login/oauth/access_token
    l10n.security.oauth2.client.userAuthorizationUri=https://github.com/login/oauth/authorize
    l10n.security.oauth2.client.useCurrentUri=false
    l10n.security.oauth2.client.preEstablishedRedirectUri=http://localhost:8080/login/oauth
    l10n.security.oauth2.resource.userInfoUri=https://api.github.com/user 

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

The [user management]({{ site.github.url }}/docs/guides/manage-users) is different
compared to when using the database. The CLI to manage users will only 
change the users in {{ site.mojito_green }} and won't interact with the `LDAP` server. This
means you can't change a user password using this command. 

As of now, there is no real need to manage users with the CLI when using `LDAP`
 as no authorization is implemented yet nor any useful information can be
 added. Later it could be used to change a user role, add a profile pic, etc.

 
