package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.RepositoryAIPrompt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositoryAIPromptRepository extends JpaRepository<RepositoryAIPrompt, Long> {

  List<RepositoryAIPrompt> findByRepositoryIdAndPromptTypeId(Long repositoryId, Long promptTypeId);

  @Query("SELECT rap.repositoryId FROM RepositoryAIPrompt rap WHERE rap.aiPromptId = :aiPromptId")
  List<Long> findRepositoryIdsByAiPromptId(@Param("aiPromptId") Long aiPromptId);
}
