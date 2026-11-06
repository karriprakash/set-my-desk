package com.setmydesk.ideas;

import com.setmydesk.shares.ShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IdeaPipelineTest {

    static final String TAILORING = "Many women in villages know tailoring but cannot find customers. "
            + "Build a WhatsApp based platform that connects home tailors with nearby customers and tracks orders. "
            + "It would help mothers earn from home.";
    static final String TAILORING_PLUS = "Home tailors in villages struggle to find customers. Add online payments and "
            + "delivery pickup to the tailoring platform so customers can pay safely and tailors get paid faster.";
    static final String SOLAR = "Farmers lose crops because pumps fail when the power is cut. "
            + "Create a shared solar pump service that farmers can book by missed call. It helps small farms irrigate on time.";

    @Autowired IdeaService service;
    @Autowired ShareService shares;
    @Autowired ProjectRepository projects;

    @Test
    void firstIdeaFoundsAProjectAndOwnsAllOfIt() {
        var out = service.submit("Lakshmi", "+919800000001", Channel.WEB, TAILORING, null);

        assertThat(out.status()).isEqualTo(IdeaStatus.ACCEPTED);
        assertThat(out.projectId()).isNotNull();
        assertThat(out.trace()).hasSize(12);
        var cap = shares.capTable(out.projectId());
        assertThat(cap.holders()).singleElement().satisfies(h -> {
            assertThat(h.name()).isEqualTo("Lakshmi");
            assertThat(h.percent()).isEqualTo(100.0);
        });
    }

    @Test
    void similarIdeaEnhancesTheExistingProjectInsteadOfCopyingIt() {
        var first = service.submit("Lakshmi", "+919800000001", Channel.WEB, TAILORING, null);
        var second = service.submit("Priya", "+919800000002", Channel.WHATSAPP, TAILORING_PLUS, null);

        assertThat(second.status()).isEqualTo(IdeaStatus.ACCEPTED);
        assertThat(second.projectId()).isEqualTo(first.projectId());
        assertThat(projects.count()).isEqualTo(1);
        assertThat(second.reply()).contains("improves the project");
        var cap = shares.capTable(first.projectId());
        assertThat(cap.holders()).hasSize(2);
        assertThat(cap.holders().stream().mapToDouble(h -> h.percent()).sum()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.05));
    }

    @Test
    void unrelatedIdeaFoundsItsOwnProject() {
        var a = service.submit("Lakshmi", "+919800000001", Channel.WEB, TAILORING, null);
        var b = service.submit("Anitha", "+919800000003", Channel.APP, SOLAR, null);
        assertThat(b.projectId()).isNotEqualTo(a.projectId());
    }

    @Test
    void sameIdeaEarnsTheSamePointsWhetherItArrivesFirstOrLast() {
        var solarFirst = service.submit("Anitha", "+919800000003", Channel.WEB, SOLAR, null);
        double early = shares.capTable(solarFirst.projectId()).totalPoints();

        service.submit("Lakshmi", "+919800000001", Channel.WEB, TAILORING, null);
        service.submit("Priya", "+919800000002", Channel.WEB, TAILORING_PLUS, null);
        var solarLater = service.submit("Meena", "+919800000004", Channel.WEB, SOLAR, null);
        double late = shares.capTable(solarLater.projectId()).totalPoints();

        assertThat(late).isEqualTo(early);
    }

    @Test
    void duplicateFromSamePersonEarnsOnce() {
        service.submit("Lakshmi", "+919800000001", Channel.WEB, TAILORING, null);
        var again = service.submit("Lakshmi", "+91 98000 00001", Channel.WHATSAPP, TAILORING, null);
        assertThat(again.status()).isEqualTo(IdeaStatus.DUPLICATE);
    }

    @Test
    void tooShortIdeaAsksForMoreDetail() {
        var out = service.submit("Rani", null, Channel.WEB, "an app", null);
        assertThat(out.status()).isEqualTo(IdeaStatus.NEEDS_DETAIL);
        assertThat(out.reply()).contains("tell us a little more");
    }

    @Test
    void contactDetailsAreRedactedFromPublicIdeaText() {
        var out = service.submit("Rani", null, Channel.WEB, TAILORING + " Call me on +91 98765 43210 or rani@example.com.", null);
        var idea = service.firingOrder(); // just to touch the bean
        assertThat(idea).hasSize(12);
        var detail = projects.findById(out.projectId()).orElseThrow();
        assertThat(detail.getCorpus()).doesNotContain("98765").doesNotContain("rani@example.com").contains("[phone removed]");
    }

    @Test
    void voiceNoteWithoutSpeechToTextWaitsForVolunteerThenEarns() {
        var parked = service.submit("Sita", "+919800000005", Channel.VOICE_NOTE, null, "target/test-audio/fake.webm");
        assertThat(parked.status()).isEqualTo(IdeaStatus.PENDING_TRANSCRIPTION);
        assertThat(parked.ideaId()).isNotNull();

        var done = service.completeTranscription(parked.ideaId(), SOLAR);
        assertThat(done.status()).isEqualTo(IdeaStatus.ACCEPTED);
        assertThat(done.ideaId()).isEqualTo(parked.ideaId());
    }
}
