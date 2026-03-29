package cc.desuka.demo.service;

import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.repository.SprintRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found with id: " + id));
    }

    public List<Sprint> getSprintsByProject(Long projectId) {
        return sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
    }

    /** Find the sprint whose date range contains today. */
    public Optional<Sprint> getActiveSprint(Long projectId) {
        return sprintRepository.findActiveByProjectId(projectId, LocalDate.now());
    }
}
