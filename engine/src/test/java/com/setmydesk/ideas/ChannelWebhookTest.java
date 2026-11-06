package com.setmydesk.ideas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChannelWebhookTest {

    @Autowired MockMvc mvc;

    @Test
    void whatsappRejectsWrongToken() throws Exception {
        mvc.perform(post("/channels/whatsapp").param("token", "nope").param("From", "whatsapp:+919800000009").param("Body", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whatsappTextIdeaGetsTwimlReplyWithShares() throws Exception {
        mvc.perform(post("/channels/whatsapp").param("token", "test-token")
                        .param("From", "whatsapp:+919800000009").param("ProfileName", "Kavya")
                        .param("Body", IdeaPipelineTest.SOLAR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<Message>Wonderful, Kavya!")))
                .andExpect(content().string(containsString("points")));
    }

    @Test
    void phoneCallIsAskedToRecord() throws Exception {
        mvc.perform(post("/channels/phone/incoming").param("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<Record action=\"/channels/phone/recorded?token=test-token\"")));
    }

    @Test
    void webAndProjectApiNeverExposePhoneNumbers() throws Exception {
        mvc.perform(post("/api/ideas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Divya\",\"phone\":\"+919811112222\",\"text\":\"" + IdeaPipelineTest.TAILORING + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].founder").value("Divya"))
                .andExpect(content().string(not(containsString("9811112222"))));
        mvc.perform(get("/api/rules")).andExpect(jsonPath("$.firingOrder.length()").value(12));
    }

    @Test
    void voiceUploadIsParkedAndVolunteerDeskIsProtected() throws Exception {
        var audio = new MockMultipartFile("audio", "idea.webm", "audio/webm", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/ideas/voice").file(audio).param("name", "Gauri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_TRANSCRIPTION"));
        mvc.perform(get("/api/ideas/pending-transcription").header("X-Volunteer-Token", "wrong"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/ideas/pending-transcription").header("X-Volunteer-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contributor").value("Gauri"));
    }
}
