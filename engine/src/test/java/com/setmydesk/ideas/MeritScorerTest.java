package com.setmydesk.ideas;

import com.setmydesk.core.TextSimilarity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeritScorerTest {

    MeritScorer scorer = new MeritScorer();

    @Test
    void richerDescriptionScoresHigher() {
        double thin = scorer.score("An app for tailors to sell clothes online").total();
        double rich = scorer.score(IdeaPipelineTest.TAILORING).total();
        assertThat(rich).isGreaterThan(thin);
        assertThat(thin).isGreaterThanOrEqualTo(MeritScorer.FLOOR);
        assertThat(rich).isLessThanOrEqualTo(1.0);
    }

    @Test
    void scoringIsDeterministic() {
        assertThat(scorer.score(IdeaPipelineTest.SOLAR)).isEqualTo(scorer.score(IdeaPipelineTest.SOLAR));
    }

    @Test
    void similarityWorksForNonLatinScripts() {
        double[] sims = TextSimilarity.similarities("మహిళలకు కుట్టు పని శిక్షణ యాప్",
                List.of("మహిళలకు కుట్టు పని శిక్షణ మరియు ఆర్డర్లు", "రైతులకు సోలార్ పంపు"));
        assertThat(sims[0]).isGreaterThan(sims[1]);
    }
}
