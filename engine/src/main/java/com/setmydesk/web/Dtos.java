package com.setmydesk.web;

import com.setmydesk.ideas.Idea;
import com.setmydesk.ideas.Project;
import com.setmydesk.shares.CapTable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Public API shapes. Phone numbers never appear here. */
public final class Dtos {
    private Dtos() {
    }

    public record SubmitIdea(@Size(max = 80) String name,
                             @Size(max = 20) String phone,
                             @NotBlank @Size(max = 10_000) String text,
                             String channel) {
    }

    public record Transcript(@NotBlank @Size(max = 10_000) String text) {
    }

    public record IdeaView(Long id, String contributor, String channel, String status, String kind, String text,
                           Double similarity, Double merit, Double points, Long projectId, String projectTitle,
                           String feedback, Instant createdAt) {
        public static IdeaView of(Idea i) {
            Project p = i.getProject();
            return new IdeaView(i.getId(), i.getContributor().getDisplayName(), i.getChannel().name(), i.getStatus().name(),
                    i.getKind() == null ? null : i.getKind().name(), i.getText(), i.getSimilarity(), i.getMerit(), i.getPoints(),
                    p == null ? null : p.getId(), p == null ? null : p.getTitle(), i.getFeedback(), i.getCreatedAt());
        }
    }

    public record ProjectSummary(Long id, String title, String founder, double totalPoints, int holders, Instant createdAt) {
    }

    public record ProjectDetail(Long id, String title, String founder, Instant createdAt, CapTable capTable, List<IdeaView> ideas) {
    }

    public record PendingVoiceNote(Long id, String contributor, String channel, String audioUrl, Instant createdAt) {
    }
}
