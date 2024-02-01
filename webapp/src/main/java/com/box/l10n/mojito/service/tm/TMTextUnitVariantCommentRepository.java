package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitVariantCommentRepository
    extends JpaRepository<TMTextUnitVariantComment, Long> {

  List<TMTextUnitVariantComment> findAllByTmTextUnitVariant_id(Long tmTextUnitVariantId);

  List<TMTextUnitVariantComment> findByTmTextUnitVariantIdIn(List<Long> tmTextUnitVariantIds);
}
