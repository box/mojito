package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.AIPrompt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AIPromptRepository extends JpaRepository<AIPrompt, Long> {

  @Query(
      "SELECT ap FROM AIPrompt ap "
          + "JOIN RepositoryAIPrompt rap ON ap.id = rap.aiPromptId "
          + "JOIN AIPromptType apt ON rap.promptTypeId = apt.id "
          + "WHERE rap.repositoryId = :repositoryId AND apt.name = :promptTypeName AND ap.deleted = false")
  List<AIPrompt> findByRepositoryIdAndPromptTypeName(
      @Param("repositoryId") Long repositoryId, @Param("promptTypeName") String promptTypeName);

  List<AIPrompt> findByDeletedFalse();

  @Query(
      "SELECT ap FROM AIPrompt ap "
          + "JOIN RepositoryAIPrompt rap ON ap.id = rap.aiPromptId "
          + "WHERE rap.repositoryId = :repositoryId AND ap.deleted = false")
  List<AIPrompt> findByRepositoryIdAndDeletedFalse(Long repositoryId);
}
