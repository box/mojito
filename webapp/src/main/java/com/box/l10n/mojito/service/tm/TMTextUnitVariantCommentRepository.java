package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitVariantCommentRepository
    extends JpaRepository<TMTextUnitVariantComment, Long> {

  @EntityGraph(value = "TMTextUnitVariantComment.legacy", type = EntityGraphType.FETCH)
  List<TMTextUnitVariantComment> findAllByTmTextUnitVariant_id(Long tmTextUnitVariantId);

  @EntityGraph(value = "TMTextUnitVariantComment.legacy", type = EntityGraphType.FETCH)
  List<TMTextUnitVariantComment> findByTmTextUnitVariantIdIn(List<Long> tmTextUnitVariantIds);
}
