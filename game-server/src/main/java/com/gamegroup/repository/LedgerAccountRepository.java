package com.gamegroup.repository;

import com.gamegroup.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    List<LedgerAccount> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    List<LedgerAccount> findByUserIdOrderByIdAsc(Long userId);

    Optional<LedgerAccount> findByIdAndUserId(Long id, Long userId);
}
