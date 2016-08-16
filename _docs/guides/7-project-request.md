---
layout: doc
title:  "Project request"
date:   2016-08-01 10:00:00 -0800
categories: guides
permalink: /docs/guides/project-request/
---

{{ site.mojito_green }} allows you to create project packages for your translation teams. You can create them at the `Project Requests` page.

### Request a translation project

![Request translation](./images/TranslationRequest.gif)

Click on `New Request`. Select the repository, for which you would like to request translations. Click on `Request Translation`. 

Your project will appear on the request page with the status `In translation`. 

{{ site.mojito_green }} will create `XLIFF` files containing all text units that need translation and place them in the directory that you specified for the repository.

### Request a review project

![Request review](./images/ReviewRequest.gif)

Click on `New Request`. Select the repository, for which you would like to request review. Click on `Request Review`. 

Your project will appear on the request page with the status `In review`. 

{{ site.mojito_green }} will create `XLIFF` files containing all text units that need review and place them in the directory that you specified for the repository.

### View and filter requests

Project Requests page will show you the details of your current and completed projects:
   
   - Project name
   - Repository
   - Word count
   - When the project was created
   - Who created the project
   - Status
  
Use the filter at the top of the page to view projects in progress (projects that haven't been imported yet), completed projects (projects that have been imported) or all projects.

### Import requests

![Import request](./images/ImportRequest.gif)

When the files are back from your translation teams, you can import them to {{ site.mojito_green }} by clicking on the `import` button. The `import` button appears when you hover over a project line. 

{{ site.mojito_green }} has quality checks that help detect some broken text units. Once the import is complete, you can go to the repository page and make sure no text units were rejected.

If text units are rejected, you will see the rejected indicator. Click on it to see the rejected strings and fix them.

### Re-import requests

![Reimport request](./images/ReimportRequest.gif)

Projects can be re-imported if needed. To re-import a project, find it among the completed projects and click on the `import` button. 

If you changed some translations since the last import and marked them as final, they won't be overwritten.

### Cancel requests

![Cancel request](./images/CancelRequest.gif)

If you created a project by mistake and need to cancel it, just click on the `cancel` button. The `cancel` button appears when you hover over a project line. All project files will be deleted from your directory.

Warning! Do not remove valid projects even if you have already imported them. You may need to re-import them later if your translators decide to correct text units.