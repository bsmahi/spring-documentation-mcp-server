package com.spring.mcp.repository;

import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CodeExample entities
 */
@Repository
public interface CodeExampleRepository extends JpaRepository<CodeExample, Long> {

    /**
     * Find all examples for a specific version
     */
    List<CodeExample> findByVersion(ProjectVersion version);

    /**
     * Find examples by version and language
     */
    List<CodeExample> findByVersionAndLanguage(ProjectVersion version, String language);

    /**
     * Find examples by version and category
     */
    List<CodeExample> findByVersionAndCategory(ProjectVersion version, String category);

    /**
     * Search examples by title
     */
    @Query("SELECT c FROM CodeExample c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CodeExample> searchByTitle(@Param("query") String query);

    /**
     * Find examples containing specific tags
     * Note: Uses PostgreSQL array operator
     */
    @Query(value = "SELECT * FROM code_examples WHERE tags && CAST(:tags AS text[])",
           nativeQuery = true)
    List<CodeExample> findByTagsContaining(@Param("tags") String[] tags);

    /**
     * Count examples by version
     */
    long countByVersion(ProjectVersion version);

    /**
     * Find distinct languages used in examples
     */
    @Query("SELECT DISTINCT c.language FROM CodeExample c ORDER BY c.language")
    List<String> findDistinctLanguages();

    /**
     * Find distinct categories
     */
    @Query("SELECT DISTINCT c.category FROM CodeExample c WHERE c.category IS NOT NULL ORDER BY c.category")
    List<String> findDistinctCategories();
}
