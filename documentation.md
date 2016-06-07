---
layout: page
title: Documentation
permalink: /docs/
---

<h3>Guides</h3>
{% for my_page in site.docs %}
  {% if my_page.title and my_page.categories contains 'guides' %}
  <a class="page-link" href="{{ my_page.url | prepend: site.github.url }}">{{ my_page.title }}</a>
  {% endif %}
{% endfor %}


<h3>References</h3>
{% for my_page in site.docs %}
  {% if my_page.title and my_page.categories contains 'refs' %}
  <a class="page-link" href="{{ my_page.url | prepend: site.github.url }}">{{ my_page.title }}</a>
  {% endif %}
{% endfor %}