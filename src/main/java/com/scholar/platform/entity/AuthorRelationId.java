package com.scholar.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorRelationId implements Serializable {
    private String author1Id;
    private String author2Id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorRelationId that = (AuthorRelationId) o;
        return Objects.equals(author1Id, that.author1Id) &&
                Objects.equals(author2Id, that.author2Id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author1Id, author2Id);
    }
}
