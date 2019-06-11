---
layout: doc
title:  "Branching (version control)"
categories: guides
permalink: /docs/guides/branching/
---

{{ site.mojito_green }} supports `branching` by simply adding an extra parameter to the `push` command.

Using branches is transparent to other tasks and won't be noticed unless voluntarily using it to 
process [pull requests](#process-pull-request--diff) and interacting with the [branch dashboard](#branch-dashboard).

The approach taken is opinionated: translations are shared between branches. Changing a translation in one branch will 
change the translation in all branches. This is the expected behavior for standard application development 
(details [here](#shared-translation-between-branches)).

### Create branches

Branches are created during the [push process]({{ site.url }}/docs/guides/string-extraction/) by just adding an extra 
parameter that specifies the branch being processed.

    mojito push -r MyRepo -b branchName

It is possible to provide the owner of the branch. This is useful when processing [pull requests](#process-pull-request).

    mojito push -r MyRepo -b branchName -bc branchOwner
    
### View branches
      
    mojito branch-view -r MyRepo
    
### Delete Branches

    mojito branch-delete -r MyRepo -n branchName    

If no branch name is provided it will attempt to delete the `null` branch (default branch created when not specifying a branch during `push`). 
 
### Primary branch 

The `master` branch is special and is used as `primary` branch. If strings are present in multiple branches including the `master` 
(common when branching) they will be shown as owned by the `master` branch even if the strings were initially created in another
branch. 

For example, if creating the `master` branch with:

```properties
# Greeting from Main UI
hello = Hello!
```
    mojito push -r MyRepo -b master

and the `feature1` branch with:

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye. Have a nice day!
```
    mojito push -r MyRepo -b feature1

then `hello` will be linked to the `master` branch and `bye` to the `feature1` branch. 

Branch information can checked in the workbench: 

![check branch info](./images/check-branch-info.gif)

When `feature1` gets merged into the `master`:

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye. Have a nice day!
```
    mojito push -r MyRepo -b master

 `bye` will then be linked to the `master` branch regardless if the 
`feature1` branch is deleted or still open. 

As a side effect, the strings won't show up anymore in the branch they are originating from in the 
[dashboard](#branch-dashboard). This might be confusing at first, but ideally branches should be short lived to avoid
performance degradation as they will be processed each time the `push`command is ran.
They should be deleted after they are merged or after a time of inactivity.

Note that the primary branch doesn't show in the branch dashboard since it is not meant to be a temporary branch. The
name of the primary branch could be made configurable later but at the moment it must be `master`.

### Process pull request / diff

One use case for using {{ site.mojito_green }}'s branches is to process "pull requests" (or "diffs") in order 
to implement an optimized continuous localization pipeline. 

Often continuous localization happens in parallel of the continuous deployment pipeline. Strings are processed only
when the commits are merged into the "master" branch and it takes some time to get them translated. 

When working in a fast paced environment where deployments can happen multiple times a day. It is likely that features
will be pushed in production without being fully localized, leading to poor experience for international users. It 
can also create inefficencies in the testing process. 

If releases are gated or less frequent, it is easier to wait for strings to be ready before publishing the feature but
last minute changes may still happen.

In any case, having strings processed early in the development process also allows to rely on the developer to get
 screenshots, to perform some internationalization testing and improve the product quality overall.

#### Trigger a localization job

This is part is independent of {{ site.mojito_green }} but the general idea is to kick off a localization job 
automatically when new strings needs to be processed. 

A developer can add a special tag in the commit message like `#translate` to have the CI pipeline react and 
trigger a job that will run the following command:

    mojito push -r MyRepo -b branchName -bc branchOwner

### Slack integration

#### Configuration

To activate Slack notifications, a Slack App/Bot is needed. It can be created [here](https://api.slack.com/apps) in Slack. 
On {{ site.mojito_green }}, the following server properties are required:

```properties
l10n.slack.token=${SLACK_TOKEN}

l10n.branchNotification.type=slack
l10n.branchNotification.slack.mojitoUrl=${MOJITO_URL}
l10n.branchNotification.slack.userEmailPattern={0}@email.org
```

`${SLACK_TOKEN}` can be found under `Features > OAuth & Permissions > Bot User OAuth Access Token` of 
the Slack app.

`userEmailPattern` is used to lookup the users. Provide the pattern corresponding to your Slack account. `{0}` is the 
username associated with the branch owner (`-bc` option). 

`${MOJITO_URL}` is the URL used to build links back to Mojito in the notifications and should be set to the URL of the
running instance.


#### Notifcaitions

After a new branch is processed, a Slack notification is sent to the branch owner to inform him that his strings were
received and are being processed.

![Slack notification received](./images/slack-notification-received.png)

Click on the `Screenshot` button to open the dashboard with the branch selected and then provide screenshots for each new 
strings. See more [details](#collecting-screenshots). 

More notifications are sent afterwards when the translations are ready (meaning the branch can be merged):

![Slack notification translated](./images/slack-notification-translated.png)

or if the source strings are changed:

![Slack notification updated](./images/slack-notification-updated.png)

### Branch dashboard

The branch dashboard is the place where developpers can check the translation status of their pull request and upload 
screenshots for the strings that were created.
    
#### Search branches

It is possible to search branches by `username` or `branch name` and filter by `deleted` status. The `Need translation` 
and `Need Screenshot` column respectively indicates how many translations are still needed and how many screenshots
should be provided. Links (on the numbers and string name) can be followed to open the workbench and check in details
the actual strings. 

![Search branch by name](./images/open-branch.png)
 
#### Collecting screenshots

Providing context to translators is key for having quality translations. In addition to adding comments in the code base
{{ site.mojito_green }} provides a simple way to collect screenshots for strings in a branch.

Once in the dashboard, it is possible to select one or multiple text units and then click on the `Add screenshot` button.

![Search branch by name](./images/add-screenshot.gif)

### Shared translations

Translations are shared between branches. Changing a translation in one branch will change the translation in all 
branches. This is the expected behavior for standard application development.

Given a source file:

```properties
# Greeting from Main UI
hello = Hello!
```
  
Assuming `Bonjour` is the French translation, when generating the localized files, notice that no branch is specified:

    mojito pull -r MyRepo

```properties
# Greeting from Main UI
hello = Bonjour
```

It is not possbible to create a branch that has a differente translation `Bonjour!` (adding the missing exclamation mark).
Branches may have different source strings (the `push` command as the branch parameter) but not translations (`pull`
command doesn't have any branch parameter).

If this is not acceptable, an alternative is to clone the repository instead but the TMs would diverge from that point 
and no tool are provided to merge branches easily.

Note that modifying the name/context/comment of an existing string in the code base leads to the creation of a new 
string. So it is tottally safe to change a placeholder in a branch while keeping the same string `name`. In that case
the translation won't be shared since the string are different.

For example, the `master` branch contains following file:

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye!
```

    mojito push -r MyRepo -b master
    
After the `push`, the repository will contain 2 strings: `hello` and `bye + Goodbye!`. Now, in a `feature1` branch, 
the `bye` string is changed to include the placeholder `username`.

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye, {username}!
```

    mojito push -r MyRepo -b feature1

After the `push`, the repository now contains 3 strings: `hello`, `bye + Goodbye!` and `bye + Goodbye, {username}!`. So 
there are no risks of breaking the application by adding/removing placeholders and sharing the translations across 
branches.
  