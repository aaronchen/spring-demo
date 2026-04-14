package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.RecurringTaskTemplate;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only recurring task template lookups. Counterpart to {@link RecurringTaskTemplateService}
 * (writes).
 */
@Service
@Transactional(readOnly = true)
public class RecurringTaskTemplateQueryService {

    private final RecurringTaskTemplateRepository templateRepository;

    public RecurringTaskTemplateQueryService(RecurringTaskTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<RecurringTaskTemplate> getTemplatesByProject(UUID projectId) {
        return templateRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public RecurringTaskTemplate getTemplateById(Long id) {
        return templateRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(RecurringTaskTemplate.class, id));
    }
}
