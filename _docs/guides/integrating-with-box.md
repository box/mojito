---
layout: doc
title:  Integrating with Box
date:   2016-06-06 15:25:25 -0800
categories: guides
permalink: /docs/guides/integrating-with-box/
---

Adding Box Platform Integration will allow project requests to be sent to the cloud.  This is a great way to exchange translation files with vendors.  When a translation request is made, XLIFFs will be sent to a Box folder where translation vendors can start their process.  When they've completed the translations, {{ site.mojito_green }} can import the translated files from Box.


### Sign up for a free Box Developer Account

Sign up [here](https://app.box.com/signup/o/default_developer_offer).

In order to add Box integration, you must first have a Box Developer account. You'll use the Box Developer account to get an API key by creating a new **Application**. {{ site.mojito_green }} will use this API key to interact with Box on your behalf.  A Free account gives access to 1 Application, 25 App Users, 50 GB Storage.  This should be plenty for most usages.  For more info about different plans, click [here](https://developers.box.com/box-platform-pricing/).


### Create a new Application
Click [here](https://app.box.com/developers/services/edit/) to create one.  An **Application** is the primary way your API to identify itself.  After you create the **Application**, you will be in the **Edit Application** page.

### Configure the Application for Box Platform

{{ site.mojito_green }} will use the **App User** to interact with the **Box Content API**.  It will do things like create folders, upload translation requests, download translated content.  By default, a {{ site.mojito_green }} **App User** is created inside the **enterprise**, for which you have granted access to your **Application**.

This is done by the following steps:

1. Set up [2-factor authentication](https://docs.box.com/docs/configuring-box-platform#section-2-set-up-two-factor-authentication) for your **Developer Account**.
2. Create a private/public key pair (See [note](#) below.  Take note of the **Public Key** ID after adding it, you'll need it later.
3. Enable [App User](https://docs.box.com/docs/configuring-box-platform#section-3-enabling-app-auth-and-app-users) for your **Application**.  It is located under OAuth2 Parameters > User Type in the **Edit Application** page.  **App User** is used for the **OAuth 2** of the **Box Content API**.  You don't need to change the **Scopes**.
4. Choose an enterprise to [grant access](https://docs.box.com/docs/configuring-box-platform#section-4-grant-access-in-enterprise-admin-console) to your **Application**.  Log into the **Enterprise Account** (ie. your **Developer Account**) and navigate to [https://app.box.com/master/settings/openbox](https://app.box.com/master/settings/openbox).  Under **Custom Applications**, you will be able to authorize.


For more detailed info about configuring Box Platform, click [here](https://docs.box.com/docs/configuring-box-platform).


### Note About the Public/Private Key

To authenticate the API key, you must create public/private key pair.  This authentication is called **App Auth** (click [here](https://docs.box.com/docs/app-auth) for more info).  **App Auth** allows your application to request OAuth 2.0 access tokens necessary to make calls to the **Box Content API**.  You would have created them in [Step 2](#configure-the-application-for-box-platform) above.  **Box** will store the public key, and you will give {{ site.mojito_green }} the private key.

For instructions on how to create the key pair: See [here](https://docs.box.com/docs/app-auth#section-1-generating-an-rsa-keypair).

>Note that Box documentation shows steps to create the key with the following:
>
>`openssl genrsa -aes256 -out private_key.pem 2048`.
>
>If Oracle JDK is used to run {{ site.mojito_green }}, this will require **JCE** to be installed
(for JDK7 see [here](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html)).
Otherwise, an exception like the folllowing will be thrown:
>
>`Caused by: java.security.InvalidKeyException: Illegal key size`.
>
>An alternative is to generate the key with the following:
>
>`openssl genrsa -aes128 -out private_key.pem 2048`


### Fill in the Required Box Client Configurations in {{ site.mojito_green }}

Navigate to `/settings` and fill in the form.

- `Client Id`: This is provided in the **Edit Application** page on Box (https://app.box.com/developers/services/edit/<###>)
- `Client Secret`: This is provided in the **Edit Application** page on Box (https://app.box.com/developers/services/edit/<###>)
- `Enterprise Id`: This is the [enterprise for which access was granted](#configure-the-application-for-box-platform) for when you were configuring the **Application**.  You can get the **Enterprise ID** at [https://app.box.com/master/settings/account](https://app.box.com/master/settings/account)
- `Public Key Id`: This is provided after setting up [App Auth](#configure-the-application-for-box-platform)
- `Private Key`: This is the key that corresponds to the Public Key that was provided to Box

![Box Integration Settings](./images/box-settings.png)

### Bootstrapping {{ site.mojito_green }} Folder Structure

By default, {{ site.mojito_green }} will create the skeleton folder structure it will use after the configurations are provided.  A `{{ site.mojito_green }}` folder containing the folder structure will be created in the root folder of the **App User** account.

![Box mojito Folder](./images/box-mojito-folder.png)

### Sharing the {{ site.mojito_green }} Folder with Vendors
Most of the time, the content of this folder will be used to exchange translation project requests / files with vendors.  At this time, {{ site.mojito_green }} does not have vendor management features.

To customize sharing of this folder to individuals, log into the **Box Developer Account** (or the **Enterprise** Admin user, for which you provided the **Enterprise ID**), navigate to the **Admin Console** ([https://app.box.com/master](https://app.box.com/master)), and follow instructions [here](https://community.box.com/t5/For-Admins/How-Do-I-Share-Files-And-Folders-From-The-Admin-Console/ta-p/211) to share with collaborators.

### Configuration in Properties
For more information about custom configuration, see [Configurations#box-platform-integration]({{ site.github.url }}/docs/refs/configurations/#box-platform-integration)

