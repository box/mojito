package com.box.l10n.mojito.service.image;

import com.box.l10n.mojito.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface ImageRepository extends JpaRepository<Image, Long>, JpaSpecificationExecutor<Image> {
    
    Optional<Image> findByName(@Param("name") String name);
}
