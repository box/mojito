---
layout: doc
title:  "LDAP Authentication"
categories: guides
permalink: /docs/guides/ldap-authentication/
---

Mojito can use `LDAP` for authentication (default uses database) 
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
 
When a user logs in via `LDAP` and there is no matching user in Mojito, a new
 user is automatically created using the information provided by the `LDAP` server.

The [user management](docs/guide/manage-users) is different
compared to when using the database. The CLI to manage users will only 
change the users in Mojito and won't interact with the `LDAP` server. This
means you can't change a user password using this command. 

As of now, there is no real need to manage users with the CLI when using `LDAP`
 as no authorization is implemented yet nor any useful information can be
 added. Later it could be used to change a user role, add a profile pic, etc.

 