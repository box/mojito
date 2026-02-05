---
layout: page
title: Documentation
permalink: /docs/
---

<p><strong>Using the webapp?</strong> Start with <a href="{{ site.url }}/docs/guides/user-roles-overview/">User Roles & Permissions</a> to see what you can do based on your role (Translator, Project Manager, Admin, or User).</p>

<h3>Guides</h3>
{% assign docs=site.docs | sort: 'path' %}
{% for my_page in docs %}
  {% if my_page.title and my_page.categories contains 'guides' %}
  <a class="page-link" href="{{ my_page.url | prepend: site.url }}">{{ my_page.title }}</a>
  {% endif %}
{% endfor %}


<h3>References</h3>
{% for my_page in docs %}
  {% if my_page.title and my_page.categories contains 'refs' %}
  <a class="page-link" href="{{ my_page.url | prepend: site.url }}">{{ my_page.title }}</a>
  {% endif %}
{% endfor %}