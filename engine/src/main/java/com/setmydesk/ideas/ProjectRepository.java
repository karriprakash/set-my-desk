package com.setmydesk.ideas;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderByTotalPointsDesc();
}
