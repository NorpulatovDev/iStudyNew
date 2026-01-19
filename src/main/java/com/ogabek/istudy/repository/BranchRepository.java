package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByNameContainingIgnoreCase(String name);
    Optional<Branch> findByName(String name);
}
