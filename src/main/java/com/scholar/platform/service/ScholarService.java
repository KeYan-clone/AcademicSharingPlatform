package com.scholar.platform.service;

import com.scholar.platform.dto.ScholarDTO;
import com.scholar.platform.entity.Scholar;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.ScholarRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScholarService {

  private final ScholarRepository scholarRepository;
  private final UserRepository userRepository;

  public List<ScholarDTO> searchByName(String name) {
    return scholarRepository.findByPublicNameContaining(name)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public ScholarDTO getScholarProfile(String userId) {
    Scholar scholar = scholarRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("学者信息不存在"));
    return toDTO(scholar);
  }

  @Transactional
  public ScholarDTO updateScholarProfile(String userId, ScholarDTO dto) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    Scholar scholar = scholarRepository.findById(userId).orElse(new Scholar());
    scholar.setUserId(userId);
    scholar.setUser(user);
    scholar.setPublicName(dto.getPublicName());
    scholar.setOrganization(dto.getOrganization());
    scholar.setTitle(dto.getTitle());
    scholar.setBio(dto.getBio());
    scholar.setAvatarUrl(dto.getAvatarUrl());

    scholar = scholarRepository.save(scholar);
    return toDTO(scholar);
  }

  private ScholarDTO toDTO(Scholar scholar) {
    ScholarDTO dto = new ScholarDTO();
    dto.setUserId(scholar.getUserId());
    dto.setPublicName(scholar.getPublicName());
    dto.setOrganization(scholar.getOrganization());
    dto.setTitle(scholar.getTitle());
    dto.setBio(scholar.getBio());
    dto.setAvatarUrl(scholar.getAvatarUrl());
    return dto;
  }
}
