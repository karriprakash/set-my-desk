package com.setmydesk.web;

import com.setmydesk.core.EngineProperties;
import com.setmydesk.ideas.IdeaRepository;
import com.setmydesk.ideas.IdeaService;
import com.setmydesk.ideas.IdeaStatus;
import com.setmydesk.ideas.Project;
import com.setmydesk.ideas.ProjectRepository;
import com.setmydesk.shares.CapTable;
import com.setmydesk.shares.ShareService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
public class ProjectController {

    private final ProjectRepository projects;
    private final IdeaRepository ideas;
    private final ShareService shares;
    private final IdeaService ideaService;
    private final EngineProperties props;

    public ProjectController(ProjectRepository projects, IdeaRepository ideas, ShareService shares,
                             IdeaService ideaService, EngineProperties props) {
        this.projects = projects;
        this.ideas = ideas;
        this.shares = shares;
        this.ideaService = ideaService;
        this.props = props;
    }

    @GetMapping("/api/projects")
    @Transactional(readOnly = true)
    public List<Dtos.ProjectSummary> list() {
        return projects.findAllByOrderByTotalPointsDesc().stream()
                .map(p -> new Dtos.ProjectSummary(p.getId(), p.getTitle(), p.getFounder().getDisplayName(),
                        Math.round(p.getTotalPoints() * 100) / 100.0, shares.capTable(p.getId()).holders().size(), p.getCreatedAt()))
                .toList();
    }

    @GetMapping("/api/projects/{id}")
    @Transactional(readOnly = true)
    public Dtos.ProjectDetail get(@PathVariable Long id) {
        Project p = projects.findById(id).orElseThrow(() -> new NoSuchElementException("No project " + id));
        CapTable cap = shares.capTable(id);
        var list = ideas.findByProjectIdAndStatusOrderByCreatedAtAsc(id, IdeaStatus.ACCEPTED).stream().map(Dtos.IdeaView::of).toList();
        return new Dtos.ProjectDetail(p.getId(), p.getTitle(), p.getFounder().getDisplayName(), p.getCreatedAt(), cap, list);
    }

    /** The full rulebook, published so anyone can check how shares are decided. */
    @GetMapping("/api/rules")
    public Map<String, Object> rules() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("cost", "Free. No money is charged or paid. Shares record contribution, not cash.");
        r.put("formula", "points = basePoints × merit × novelty");
        r.put("basePoints", props.basePoints());
        r.put("merit", "0.2 + 0.8 × (0.30·detail + 0.25·problem + 0.20·solution + 0.15·audience + 0.10·structure)");
        r.put("novelty", "1.0 for a new project; (1 − similarity) when improving an existing project");
        r.put("enhancementThreshold", props.enhancementThreshold());
        r.put("timeInvariance", "The formula has no date or order input. The same idea earns the same points whether it arrives first or last.");
        r.put("sharePercent", "your points ÷ all points in the project. Percentages rebalance as the project grows; points never shrink.");
        r.put("ledger", "Append-only. Shares are never edited or removed.");
        r.put("firingOrder", ideaService.firingOrder());
        return r;
    }
}
