package com.setmydesk.shares;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareEntryRepository extends JpaRepository<ShareEntry, Long> {
    List<ShareEntry> findByProjectIdOrderByCreatedAtAsc(Long projectId);
}
