package com.gamegroup.config;

import com.gamegroup.entity.LedgerAccount;
import com.gamegroup.entity.LedgerCategory;
import com.gamegroup.repository.LedgerAccountRepository;
import com.gamegroup.repository.LedgerCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerDataInitializer implements CommandLineRunner {

    private static final Long SYSTEM_USER_ID = 0L;

    private final LedgerCategoryRepository ledgerCategoryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initCategory("餐饮", 1, "food", 1);
        initCategory("交通", 1, "transport", 2);
        initAccount("现金", "Cash", "cash", 1);
    }

    private void initCategory(String name, Integer type, String icon, Integer sortOrder) {
        boolean exists = ledgerCategoryRepository
                .findByUserIdAndTypeOrderBySortOrderAscIdAsc(SYSTEM_USER_ID, type)
                .stream()
                .anyMatch(item -> name.equals(item.getName()));
        if (exists) {
            return;
        }
        LedgerCategory category = new LedgerCategory();
        category.setUserId(SYSTEM_USER_ID);
        category.setName(name);
        category.setType(type);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        ledgerCategoryRepository.save(category);
        log.info("Init ledger category: name={}, type={}", name, type);
    }

    private void initAccount(String name, String type, String icon, Integer sortOrder) {
        boolean exists = ledgerAccountRepository.findByUserIdOrderBySortOrderAscIdAsc(SYSTEM_USER_ID)
                .stream()
                .anyMatch(item -> name.equals(item.getName()));
        if (exists) {
            return;
        }
        LedgerAccount account = new LedgerAccount();
        account.setUserId(SYSTEM_USER_ID);
        account.setName(name);
        account.setType(type);
        account.setIcon(icon);
        account.setSortOrder(sortOrder);
        account.setBalance(BigDecimal.ZERO);
        ledgerAccountRepository.save(account);
        log.info("Init ledger account: name={}", name);
    }
}
