package com.setmydesk.ideas;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContributorRepository extends JpaRepository<Contributor, Long> {
    Optional<Contributor> findByPhone(String phone);
}
