package com.scholar.platform.repository;

import com.scholar.platform.entity.AuthorRelation;
import com.scholar.platform.entity.AuthorRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRelationRepository extends JpaRepository<AuthorRelation, AuthorRelationId> {
    List<AuthorRelation> findByAuthor1Name(String author1Name);
    List<AuthorRelation> findByAuthor2Name(String author2Name);
}
