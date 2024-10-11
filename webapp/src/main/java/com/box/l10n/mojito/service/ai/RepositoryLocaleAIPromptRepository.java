package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositoryLocaleAIPromptRepository
    extends JpaRepository<RepositoryLocaleAIPrompt, Long> {

  @Query(
      "SELECT count(rlap.id) FROM RepositoryLocaleAIPrompt rlap "
          + "JOIN AIPrompt aip ON rlap.aiPrompt.id = aip.id "
          + "JOIN AIPromptType aipt ON aip.promptType.id = aipt.id "
          + "WHERE rlap.repository.id = :repositoryId AND rlap.disabled = false AND aip.deleted = false AND aipt.name = :promptType")
  Long findCountOfActiveRepositoryPromptsByType(
      @Param("repositoryId") Long repositoryId, @Param("promptType") String promptType);

  @Query(
      "SELECT rlap FROM RepositoryLocaleAIPrompt rlap "
          + "JOIN rlap.aiPrompt aip "
          + "JOIN aip.promptType aipt "
          + "WHERE rlap.repository.id = :repositoryId AND aip.deleted = false AND aipt.name = :promptType")
  List<RepositoryLocaleAIPrompt> getActivePromptsByRepositoryAndPromptType(
      @Param("repositoryId") Long repositoryId, @Param("promptType") String promptType);
}
