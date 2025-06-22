package com.box.l10n.mojito.service.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TextUnitElasticsearchRepository
    extends ElasticsearchRepository<TextUnitElasticsearch, String> {}
