package com.spring.mcp.repository;

import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.model.entity.DocumentationType;
import com.spring.mcp.model.entity.ProjectVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DocumentationLink entities
 */
@Repository
public interface DocumentationLinkRepository extends JpaRepository<DocumentationLink, Long> {

    /**
     * Find all links for a specific version
     */
    List<DocumentationLink> findByVersion(ProjectVersion version);

    /**
     * Find all active links for a specific version
     */
    List<DocumentationLink> findByVersionAndIsActiveTrue(ProjectVersion version);

    /**
     * Find links by version and documentation type
     */
    List<DocumentationLink> findByVersionAndDocType(ProjectVersion version, DocumentationType docType);

    /**
     * Find active links by version and type
     */
    List<DocumentationLink> findByVersionAndDocTypeAndIsActiveTrue(
        ProjectVersion version,
        DocumentationType docType
    );

    /**
     * Find link by URL
     */
    Optional<DocumentationLink> findByUrl(String url);

    /**
     * Search documentation links by title
     */
    @Query("SELECT d FROM DocumentationLink d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) AND d.isActive = true")
    List<DocumentationLink> searchByTitle(@Param("query") String query);

    /**
     * Count active links for a version
     */
    long countByVersionAndIsActiveTrue(ProjectVersion version);

    /**
     * Find all links for a specific version ID
     */
    List<DocumentationLink> findByVersionId(Long versionId);
}
