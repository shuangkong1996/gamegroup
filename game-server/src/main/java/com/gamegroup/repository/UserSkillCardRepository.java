package com.gamegroup.repository;

import com.gamegroup.entity.UserSkillCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSkillCardRepository extends JpaRepository<UserSkillCard, Long> {

    List<UserSkillCard> findByUserId(Long userId);
}
