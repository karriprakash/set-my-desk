package com.setmydesk.ideas;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IdeaRepository extends JpaRepository<Idea, Long> {
    List<Idea> findByStatusOrderByCreatedAtAsc(IdeaStatus status);
    List<Idea> findByProjectIdAndStatusOrderByCreatedAtAsc(Long projectId, IdeaStatus status);
    List<Idea> findByContributorIdAndStatus(Long contributorId, IdeaStatus status);
}
