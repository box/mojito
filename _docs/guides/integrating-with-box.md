---
layout: doc
title:  Integrating with Box
date:   2016-06-06 15:25:25 -0800
categories: guides
permalink: /docs/guides/integrating-with-box/
---

Adding Box Platform Integration will allow project requests to be sent to the cloud.  This is a great way to exchange translation files with vendors.  When a translation request is made, XLIFFs will be sent to a Box folder where translation vendors can start their process.  When they've completed the translations, Mojito can import the updated translations inside Box.


### Sign up a free Box Developer Account

Sign up [here](https://app.box.com/signup/o/default_developer_offer).

In order to add Box integration, you must first have a Box Developer account. You'll use the Box Developer account to get an API key by creating a new **Application**. Mojito will use this API key to interact with Box on your behalf.  A Free account gives access to 1 Application, 25 App Users, 50 GB Storage.  This should be plenty for most usages.  For more info about different plans, click [here](https://developers.box.com/box-platform-pricing/).


### Create a new Application
Click [here](https://app.box.com/developers/services/edit/) to create one.  An **Application** is the primary way your API to identify itself.

### Configure the Application for Box Platform

Mojito will use the **App User** to interact with the **Box Content API**.  It will do things like create folders, upload translation requests, download translated content.  A Mojito **App User** is  created initially for the **enterprise** that you have granted access to your **Application** for.

This is done by the following steps:

1. Set up [2-factor authentication](https://docs.box.com/docs/configuring-box-platform#section-2-set-up-two-factor-authentication) for your **Developer Account**.
2. Set up [App Auth](https://docs.box.com/docs/app-auth).  **App Auth** allows your application to request OAuth 2.0 access tokens necessary to make calls to the **Box Content API**.
2. Enable [App Users](https://docs.box.com/docs/configuring-box-platform#section-3-enabling-app-auth-and-app-users) for your Application.  **App User** is used for the **OAuth** of the **Box Content API**.
3. Choose an enterprise to [grant access](https://docs.box.com/docs/configuring-box-platform#section-4-grant-access-in-enterprise-admin-console) to.


For more detailed info about the configuring Box Platform, click [here](https://docs.box.com/docs/configuring-box-platform).


### Note about the Public/Private key

To authenticate the API key, you must create public/private key pair.  You would have created them in [Step 2](#configure-the-application-for-box-platform) above.  **Box** will store the public key and you will give **Mojito** the private key.

>Note that Box documentation shows steps to create the key with the following:
>
>`openssl genrsa -aes256 -out private_key.pem 2048`.
>
>If Oracle JDK is used to run Mojito, this will require **JCE** to be installed
(for JDK7 see [here](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html))
Otherwise, an exception like the folllowing will be thrown:
>
>`Caused by: java.security.InvalidKeyException: Illegal key size`.
>
>An alternative is to generate the key with the following:
>
>`openssl genrsa -aes128 -out private_key.pem 2048`


### Fill in the required Box Client Configurations in Mojito

Navigate to `/settings/box` and fill in the form.

![Box Integration Settings](./images/box-settings.png)
