package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectTextUnit;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectTextUnitRepository
    extends JpaRepository<ReviewProjectTextUnit, Long> {

    // TODO(ja) this is doing N+1 queries - tm_text_unit_statistic relationship is lazy but Hibernate
    // fetch anyway. It is plain bad. but removing the relationship might have other side effect
    // and so we can't do it part of this. Adding statistic to the graph to fetch for now to avoid N+1
    // select
    // t1_0.id,
    //         t1_0.created_date,
    //         t1_0.last_day_usage_count,
    //         t1_0.last_modified_date,
    //         t1_0.last_period_usage_count,
    //         t1_0.last_seen_date,
    //         t1_0.tm_text_unit_id
    // from
    // tm_text_unit_statistic t1_0
    // where
    // t1_0.tm_text_unit_id=?
  @EntityGraph(
      value = "ReviewProjectTextUnit.detail", type = EntityGraph.EntityGraphType.FETCH)
  List<ReviewProjectTextUnit> findByReviewProjectId(Long reviewProjectId);
}
