package com.lostfound.repo;

import com.lostfound.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByClaimantOrderByCreatedAtDesc(User claimant);
    List<Claim> findByItem_ReporterOrderByCreatedAtDesc(User reporter);
    List<Claim> findByItemAndStatus(Item item, ClaimStatus status);
    List<Claim> findByItem(Item item);
    boolean existsByItemAndClaimantAndStatus(Item item, User claimant, ClaimStatus status);
    void deleteByItem(Item item);
}
