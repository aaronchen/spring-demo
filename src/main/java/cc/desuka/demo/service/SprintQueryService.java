package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.repository.SprintRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only sprint lookups. Counterpart to {@link SprintService} (writes). */
@Service
@Transactional(readOnly = true)
public class SprintQueryService {

    private final SprintRepository sprintRepository;

    public SprintQueryService(SprintRepository sprintRepository) {
        this.sprintRepository = sprintRepository;
    }

    public Sprint getSprintById(Long id) {
        return sprintRepository
                .findWithProjectById(id)
                .orElseThrow(() -> new EntityNotFoundException(Sprint.class, id));
    }

    public List<Sprint> getSprintsByProject(UUID projectId) {
        return sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
    }

    /** Find the sprint whose date range contains today. */
    public Optional<Sprint> getActiveSprint(UUID projectId) {
        return sprintRepository.findActiveByProjectId(projectId, LocalDate.now());
    }
}
