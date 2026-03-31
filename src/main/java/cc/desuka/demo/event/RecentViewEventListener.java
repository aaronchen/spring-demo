package cc.desuka.demo.event;

import cc.desuka.demo.model.RecentView;
import cc.desuka.demo.service.RecentViewService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecentViewEventListener {

    private final RecentViewService recentViewService;

    public RecentViewEventListener(RecentViewService recentViewService) {
        this.recentViewService = recentViewService;
    }

    @TransactionalEventListener
    public void onTaskUpdated(TaskUpdatedEvent event) {
        recentViewService.updateTitle(
                RecentView.TYPE_TASK, event.task().getId(), event.task().getTitle(), event.actor());
    }

    @TransactionalEventListener
    public void onProjectUpdated(ProjectUpdatedEvent event) {
        recentViewService.updateTitle(
                RecentView.TYPE_PROJECT,
                event.project().getId(),
                event.project().getName(),
                event.actor());
    }
}
