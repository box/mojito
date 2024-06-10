package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.AIPromptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AIPromptTypeRepository extends JpaRepository<AIPromptType, Long> {

  AIPromptType findByName(String name);
}
