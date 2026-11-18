package com.crm.core.service;

import com.crm.core.dto.PersonDto;
import com.crm.core.dto.lead.*;
import com.crm.core.entity.Lead;
import com.crm.core.entity.LeadEvent;
import com.crm.core.entity.Person;
import com.crm.core.entity.Pipeline;
import com.crm.core.entity.PipelineStage;
import com.crm.core.entity.enums.LeadEventType;
import com.crm.core.entity.enums.LeadStatus;
import com.crm.core.entity.enums.LostReasonMode;
import com.crm.core.entity.enums.StaffRole;
import com.crm.core.exception.DuplicateException;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.LeadEventRepository;
import com.crm.core.repository.LeadRepository;
import com.crm.core.repository.PersonRepository;
import com.crm.core.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Лиды и их движение по воронке продаж. Каждое изменение записывается в историю лида. */
@Service
@RequiredArgsConstructor
public class LeadService {

    /** Кого можно назначить ответственным за лид. */
    private static final Set<StaffRole> LEAD_OWNERS = Set.of(StaffRole.MANAGER, StaffRole.ADMIN);

    private final LeadRepository leadRepository;
    private final LeadEventRepository eventRepository;
    private final PersonRepository personRepository;
    private final PersonService personService;
    private final PipelineService pipelineService;
    private final UserService userService;

    public record Filter(UUID pipelineId, UUID stageId, LeadStatus status, UUID managerId,
                         boolean mine, boolean archived, String query) {
    }

    @Transactional(readOnly = true)
    public List<LeadView> search(Filter filter) {
        UUID managerId = filter.mine() ? CurrentUser.get().id() : filter.managerId();
        String query = filter.query() == null ? "" : filter.query().trim();
        return leadRepository.search(filter.archived(), filter.pipelineId(), filter.stageId(), filter.status(),
                        managerId, query)
                .stream().map(LeadView::of).toList();
    }

    @Transactional(readOnly = true)
    public LeadView get(UUID id) {
        return LeadView.of(find(id));
    }

    @Transactional(readOnly = true)
    public List<LeadEventView> history(UUID id) {
        find(id); // 404 для чужого/несуществующего лида
        return eventRepository.findAllByLeadIdOrderByCreatedAtAsc(id).stream().map(LeadEventView::of).toList();
    }

    @Transactional
    public LeadView create(CreateLeadRequest request) {
        String phone = PhoneNumbers.normalize(request.phoneNumber());
        String messengerId = blankToNull(request.messengerId());
        checkDuplicates(null, phone, messengerId);

        Pipeline pipeline = request.pipelineId() == null
                ? pipelineService.defaultPipeline()
                : pipelineService.requirePipeline(request.pipelineId());
        PipelineStage stage = request.stageId() == null
                ? pipelineService.firstStage(pipeline.getId())
                : pipelineService.requireStage(pipeline.getId(), request.stageId());

        Lead lead = leadRepository.saveAndFlush(Lead.builder() // flush заполняет createdAt
                .name(request.name().trim())
                .phoneNumber(phone)
                .messengerId(messengerId)
                .source(blankToNull(request.source()))
                .pipelineId(pipeline.getId())
                .stageId(stage.getId())
                .status(LeadStatus.OPEN)
                .managerId(resolveManager(request.managerId()))
                .archived(false)
                .build());

        record(lead, LeadEvent.builder().type(LeadEventType.CREATED)
                .toStageId(stage.getId()).managerId(lead.getManagerId()));
        return LeadView.of(lead);
    }

    @Transactional
    public LeadView update(UUID id, UpdateLeadRequest request) {
        Lead lead = findActive(id);
        String phone = PhoneNumbers.normalize(request.phoneNumber());
        String messengerId = blankToNull(request.messengerId());
        checkDuplicates(lead.getId(), phone, messengerId);

        lead.setName(request.name().trim());
        lead.setPhoneNumber(phone);
        lead.setMessengerId(messengerId);
        lead.setSource(blankToNull(request.source()));
        record(lead, LeadEvent.builder().type(LeadEventType.UPDATED));
        return LeadView.of(lead);
    }

    @Transactional
    public LeadView moveToStage(UUID id, UUID stageId) {
        Lead lead = findOpen(id);
        PipelineStage stage = pipelineService.requireStage(lead.getPipelineId(), stageId);
        if (stage.getId().equals(lead.getStageId())) {
            return LeadView.of(lead);
        }
        UUID from = lead.getStageId();
        lead.setStageId(stage.getId());
        record(lead, LeadEvent.builder().type(LeadEventType.STAGE_CHANGED).fromStageId(from).toStageId(stage.getId()));
        return LeadView.of(lead);
    }

    @Transactional
    public LeadView assignManager(UUID id, UUID managerId) {
        Lead lead = findActive(id);
        if (managerId != null) {
            userService.requireActiveStaff(managerId, LEAD_OWNERS,
                    "Ответственным может быть только активный менеджер или администратор");
        }
        if (Objects.equals(lead.getManagerId(), managerId)) {
            return LeadView.of(lead);
        }
        lead.setManagerId(managerId);
        record(lead, LeadEvent.builder().type(LeadEventType.MANAGER_CHANGED).managerId(managerId));
        return LeadView.of(lead);
    }

    /**
     * Успешная продажа: лид закрывается и связывается с карточкой человека-ученика.
     * Если человек с таким телефоном или мессенджером уже есть (повторное обращение), используется его карточка.
     */
    @Transactional
    public LeadView win(UUID id) {
        Lead lead = findOpen(id);

        Person person = lead.getPhoneNumber() == null ? null
                : personRepository.findFirstByPhoneNumber(lead.getPhoneNumber()).orElse(null);
        if (person == null && lead.getMessengerId() != null) {
            person = personRepository.findFirstByMessengerId(lead.getMessengerId()).orElse(null);
        }
        if (person == null) {
            person = personService.createPerson(personFromLead(lead));
        } else if (!person.isStudent()) {
            person.setStudent(true);
        }

        lead.setStatus(LeadStatus.WON);
        lead.setPersonId(person.getId());
        lead.setClosedAt(LocalDateTime.now());
        record(lead, LeadEvent.builder().type(LeadEventType.WON));
        return LeadView.of(lead);
    }

    @Transactional
    public LeadView lose(UUID id, LoseLeadRequest request) {
        Lead lead = findOpen(id);
        Pipeline pipeline = pipelineService.requirePipeline(lead.getPipelineId());
        UUID reasonId = request == null ? null : request.lostReasonId();
        String comment = request == null ? null : blankToNull(request.comment());

        if (pipeline.getLostReasonMode() == LostReasonMode.DISABLED) {
            reasonId = null;
        } else if (reasonId != null) {
            pipelineService.requireLostReason(pipeline.getId(), reasonId);
        } else if (pipeline.getLostReasonMode() == LostReasonMode.REQUIRED) {
            throw new IllegalArgumentException("Укажите причину проигрыша");
        }

        lead.setStatus(LeadStatus.LOST);
        lead.setLostReasonId(reasonId);
        lead.setLostComment(comment);
        lead.setClosedAt(LocalDateTime.now());
        record(lead, LeadEvent.builder().type(LeadEventType.LOST).lostReasonId(reasonId).comment(comment));
        return LeadView.of(lead);
    }

    /** Вернуть закрытый лид в работу (например, проигранный клиент передумал). */
    @Transactional
    public LeadView reopen(UUID id) {
        Lead lead = findActive(id);
        if (lead.getStatus() == LeadStatus.OPEN) {
            throw new IllegalArgumentException("Лид уже в работе");
        }
        lead.setStatus(LeadStatus.OPEN);
        lead.setLostReasonId(null);
        lead.setLostComment(null);
        lead.setClosedAt(null);
        record(lead, LeadEvent.builder().type(LeadEventType.REOPENED).toStageId(lead.getStageId()));
        return LeadView.of(lead);
    }

    @Transactional
    public LeadView setArchived(UUID id, boolean archived) {
        Lead lead = find(id);
        if (lead.isArchived() == archived) {
            return LeadView.of(lead);
        }
        lead.setArchived(archived);
        record(lead, LeadEvent.builder().type(archived ? LeadEventType.ARCHIVED : LeadEventType.UNARCHIVED));
        return LeadView.of(lead);
    }

    // ---------- Внутреннее ----------

    /** Точное совпадение телефона или мессенджера — дубль; ответ содержит id существующего лида. */
    private void checkDuplicates(UUID selfId, String phone, String messengerId) {
        if (phone != null) {
            leadRepository.findFirstByPhoneNumber(phone)
                    .filter(existing -> !existing.getId().equals(selfId))
                    .ifPresent(existing -> {
                        throw new DuplicateException("Лид с таким номером телефона уже существует: "
                                + existing.getName() + (existing.isArchived() ? " (в архиве)" : ""), existing.getId());
                    });
        }
        if (messengerId != null) {
            leadRepository.findFirstByMessengerId(messengerId)
                    .filter(existing -> !existing.getId().equals(selfId))
                    .ifPresent(existing -> {
                        throw new DuplicateException("Лид с таким идентификатором мессенджера уже существует: "
                                + existing.getName() + (existing.isArchived() ? " (в архиве)" : ""), existing.getId());
                    });
        }
    }

    /** Явно указанный ответственный, иначе — создатель лида, если он менеджер. */
    private UUID resolveManager(UUID requested) {
        if (requested != null) {
            userService.requireActiveStaff(requested, LEAD_OWNERS,
                    "Ответственным может быть только активный менеджер или администратор");
            return requested;
        }
        CurrentUser user = CurrentUser.get();
        return user.hasRole(StaffRole.MANAGER) ? user.id() : null;
    }

    private void record(Lead lead, LeadEvent.LeadEventBuilder event) {
        eventRepository.save(event.leadId(lead.getId()).actorId(CurrentUser.get().id()).build());
    }

    private Lead find(UUID id) {
        return leadRepository.findById(id).orElseThrow(() -> new NotFoundException("Лид не найден"));
    }

    private Lead findActive(UUID id) {
        Lead lead = find(id);
        if (lead.isArchived()) {
            throw new IllegalArgumentException("Лид в архиве — сначала верните его из архива");
        }
        return lead;
    }

    private Lead findOpen(UUID id) {
        Lead lead = findActive(id);
        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new IllegalArgumentException("Лид уже закрыт — сначала верните его в работу");
        }
        return lead;
    }

    private static PersonDto personFromLead(Lead lead) {
        String[] nameParts = lead.getName().trim().split("\\s+", 2);
        PersonDto dto = new PersonDto();
        dto.setFirstName(nameParts[0]);
        dto.setLastName(nameParts.length > 1 ? nameParts[1] : null);
        dto.setPhoneNumber(lead.getPhoneNumber());
        dto.setMessengerId(lead.getMessengerId());
        dto.setStudent(true);
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
