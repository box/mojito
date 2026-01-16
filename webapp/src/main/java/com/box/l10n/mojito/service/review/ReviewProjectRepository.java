package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProject;
import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectRepository extends JpaRepository<ReviewProject, Long> {

  @EntityGraph(attributePaths = {"repositories", "locale"})
  List<ReviewProject> findByStatusOrderByCreatedDateDesc(ReviewProjectStatus status);

  @Override
  @EntityGraph(attributePaths = {"repositories", "locale"})
  Optional<ReviewProject> findById(Long id);
}
