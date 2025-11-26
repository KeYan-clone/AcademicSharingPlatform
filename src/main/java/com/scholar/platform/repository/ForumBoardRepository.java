package com.scholar.platform.repository;

import com.scholar.platform.entity.ForumBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumBoardRepository extends JpaRepository<ForumBoard, String> {

  Optional<ForumBoard> findByName(String name);

  List<ForumBoard> findByType(Integer type);
}
