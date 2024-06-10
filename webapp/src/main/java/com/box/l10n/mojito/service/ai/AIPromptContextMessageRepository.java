package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.AIPromptContextMessage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AIPromptContextMessageRepository
    extends JpaRepository<AIPromptContextMessage, Long> {

  Optional<AIPromptContextMessage> findByAiPromptIdAndOrderIndexAndDeleted(
      Long aiPrompt_id, Integer orderIndex, boolean deleted);
}
