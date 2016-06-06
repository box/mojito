package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.List;

/**
 *
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitVariantCommentRepository extends JpaRepository<TMTextUnitVariantComment, Long> {

    public List<TMTextUnitVariantComment> findAllByTmTextUnitVariant_id(Long tmTextUnitVariantId);

    public List<TMTextUnitVariantComment> findByTmTextUnitVariantIdIn(List<Long> tmTextUnitVariantIds);

}
