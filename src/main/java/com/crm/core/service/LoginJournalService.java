package com.crm.core.service;

import com.crm.core.entity.LoginAttempt;
import com.crm.core.entity.enums.LoginFailureReason;
import com.crm.core.repository.LoginAttemptRepository;
import com.crm.core.security.ClientInfo;
import com.crm.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Журнал успешных и неуспешных входов (только добавление записей). */
@Service
@RequiredArgsConstructor
public class LoginJournalService {

    private final LoginAttemptRepository repository;

    public void staffSuccess(UUID tenantId, UUID userId, String email, ClientInfo client) {
        save(tenantId, userId, false, email, null, client);
    }

    public void staffFailure(UUID tenantId, UUID userId, String email, LoginFailureReason reason, ClientInfo client) {
        save(tenantId, userId, false, email, reason, client);
    }

    public void platformAttempt(String email, LoginFailureReason reasonOrNull, ClientInfo client) {
        save(null, null, true, email, reasonOrNull, client);
    }

    /** Журнал входов текущей школы — для её администратора. */
    public Page<LoginAttempt> currentOrganizationJournal(Pageable pageable) {
        return repository.findAllByTenantIdOrderByCreatedAtDesc(TenantContext.require(), pageable);
    }

    private void save(UUID tenantId, UUID userId, boolean platformAdmin, String email,
                      LoginFailureReason reason, ClientInfo client) {
        repository.save(LoginAttempt.builder()
                .tenantId(tenantId)
                .userId(userId)
                .platformAdmin(platformAdmin)
                .email(email)
                .success(reason == null)
                .failureReason(reason)
                .ipAddress(client.ipAddress())
                .userAgent(client.userAgent())
                .build());
    }
}
