---
layout: doc
title:  "Manage Users"
categories: guides
permalink: /docs/guides/manage-users/
---

The default authentication used by {{ site.mojito_green }} relies on the database

    l10n.security.authenticationType=DATABASE
    
User information is stored in database and all the authentication process
is handle by the server. Managing users is done 
via the CLI.

Alternatively, [LDAP]({{ site.github.url }}/docs/guides/ldap-authentication) can be used.
       
### Bootstraping

{{ site.mojito_green }} initially is set up with one default user `admin/ChangeMe`.
You can override the default user settings. These values are only respected on initial bootstrapping.

    l10n.bootstrap.defaultUser.username=admin
    l10n.bootstrap.defaultUser.password=ChangeMe

### Add new user

    mojito user-create  
        --username ${USERNAME}
        --surname ${SURNAME}
        --given-name ${GIVEN_NAME} 
        --common-name ${COMMON_NAME}

Enter password when promted.

### Update password
    mojito user-update --username ${USERNAME} --password

Enter password when promted.

### Delete user
    mojito user-delete --username ${USERNAME}