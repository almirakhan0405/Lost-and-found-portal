package com.lostfound.repo;

import com.lostfound.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByReporterOrderByCreatedAtDesc(User reporter);
    List<Item> findByTypeAndStatus(ItemType type, ItemStatus status);
    long countByType(ItemType type);
    long countByStatus(ItemStatus status);
}
