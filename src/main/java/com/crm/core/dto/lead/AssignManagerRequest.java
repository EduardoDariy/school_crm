package com.crm.core.dto.lead;

import java.util.UUID;

/** managerId = null снимает ответственного. */
public record AssignManagerRequest(UUID managerId) {
}
