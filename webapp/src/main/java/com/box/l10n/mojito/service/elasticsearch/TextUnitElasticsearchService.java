package com.box.l10n.mojito.service.elasticsearch;

import static co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
docker run -d --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.13.4

 */
@Component
public class TextUnitElasticsearchService {

  static Logger logger = LoggerFactory.getLogger(TextUnitElasticsearchService.class);

  TextUnitElasticsearchRepository textUnitElasticsearchRepository;

  ElasticsearchClient elasticsearchClient;

  public TextUnitElasticsearchService(
      TextUnitElasticsearchRepository textUnitElasticsearchRepository,
      ElasticsearchClient elasticsearchClient) {
    this.textUnitElasticsearchRepository = textUnitElasticsearchRepository;
    this.elasticsearchClient = elasticsearchClient;
  }

  public void indexTextUnitDTOs(List<TextUnitDTO> textUnitDTOs) {
    logger.debug("Index textUnitDTOs, size: {}", textUnitDTOs.size());
    List<TextUnitElasticsearch> list =
        textUnitDTOs.stream()
            .map(
                t -> {
                  TextUnitElasticsearch textUnitElasticsearch = new TextUnitElasticsearch();
                  textUnitElasticsearch.setId(t.getTmTextUnitVariantId());
                  textUnitElasticsearch.setRepositoryName(t.getRepositoryName());
                  textUnitElasticsearch.setName(t.getName());
                  textUnitElasticsearch.setSource(t.getSource());
                  if (t.getTarget() != null) {
                    TextUnitElasticsearchFieldMapper.setTargetForLocale(
                        textUnitElasticsearch, t.getTargetLocale(), t.getTarget());
                  }
                  return textUnitElasticsearch;
                })
            .toList();

    textUnitElasticsearchRepository.saveAll(list);
  }

  public List<Hit<TextUnitElasticsearch>> fuzzySearchByTarget(String target, List<String> locales) {
    List<String> fields =
        (locales == null || locales.isEmpty())
            ? TextUnitElasticsearchFieldMapper.getAllTargetFields()
            : locales.stream()
                .map(TextUnitElasticsearchFieldMapper::getFieldNameForSearch)
                .distinct()
                .toList();

    Query multiMatchQuery =
        MultiMatchQuery.of(m -> m.query(target).fields(fields).fuzziness("AUTO").type(BestFields))
            ._toQuery();

    final Query filterQuery;
    if (locales != null && !locales.isEmpty()) {
      List<FieldValue> localeValues = locales.stream().map(FieldValue::of).toList();
      filterQuery =
          TermsQuery.of(t -> t.field("targetLocale").terms(v -> v.value(localeValues)))._toQuery();
    } else {
      filterQuery = null;
    }

    Query query;
    if (filterQuery != null) {
      query = BoolQuery.of(b -> b.filter(filterQuery).must(multiMatchQuery))._toQuery();
    } else {
      query = multiMatchQuery;
    }
    SearchRequest request = SearchRequest.of(s -> s.index("text-units").query(query));

    SearchResponse<TextUnitElasticsearch> response = null;
    try {
      response = elasticsearchClient.search(request, TextUnitElasticsearch.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return response.hits().hits();
  }

  public Buckets categorizeText() {

    SearchResponse<Void> rsp = null;
    try {
      rsp =
          elasticsearchClient.search(
              s ->
                  s.index("text-units")
                      .size(0)
                      .query(q -> q.term(t -> t.field("targetLocale").value("en")))
                      .aggregations(
                          "all",
                          a ->
                              a.categorizeText(
                                  ct -> ct.field("source").similarityThreshold(80).size(10_000))),
              Void.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return rsp.aggregations().get("all")._custom().to(Buckets.class);
  }

  public record Buckets(List<Bucket> buckets) {
    public record Bucket(
        String key,
        long doc_count,
        String regex,
        @JsonProperty("max_matching_length") int maxMatchingLength) {}
  }
}
