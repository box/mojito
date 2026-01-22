package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectRequestRepository extends JpaRepository<ReviewProjectRequest, Long> {}
