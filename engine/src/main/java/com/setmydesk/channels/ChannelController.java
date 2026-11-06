package com.setmydesk.channels;

import com.setmydesk.ideas.Channel;
import com.setmydesk.ideas.IdeaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Webhooks for messaging providers (Twilio format; most providers can be adapted to it).
 * Point the WhatsApp sandbox/number at {@code /channels/whatsapp?token=...} and the phone
 * number's voice webhook at {@code /channels/phone/incoming?token=...}.
 */
@RestController
@RequestMapping(value = "/channels", produces = MediaType.APPLICATION_XML_VALUE)
public class ChannelController {

    private static final Logger log = LoggerFactory.getLogger(ChannelController.class);

    private final IdeaService ideas;
    private final AudioStore audio;
    private final Notifier notifier;
    private final WebhookGuard guard;
    private final ExecutorService background = Executors.newVirtualThreadPerTaskExecutor();

    public ChannelController(IdeaService ideas, AudioStore audio, Notifier notifier, WebhookGuard guard) {
        this.ideas = ideas;
        this.audio = audio;
        this.notifier = notifier;
        this.guard = guard;
    }

    @PostMapping("/whatsapp")
    public String whatsapp(@RequestParam String token,
                           @RequestParam("From") String from,
                           @RequestParam(value = "Body", required = false) String body,
                           @RequestParam(value = "ProfileName", required = false) String profileName,
                           @RequestParam(value = "NumMedia", defaultValue = "0") int numMedia,
                           @RequestParam(value = "MediaUrl0", required = false) String mediaUrl,
                           @RequestParam(value = "MediaContentType0", required = false) String mediaType) {
        guard.requireWebhook(token);
        String phone = from.replaceFirst("^whatsapp:", "");

        boolean voiceNote = numMedia > 0 && mediaUrl != null && mediaType != null && mediaType.startsWith("audio/");
        if (!voiceNote) {
            return TwiML.message(ideas.submit(profileName, phone, Channel.WHATSAPP, body, null).reply());
        }
        // Downloading + transcribing can exceed the provider's timeout, so answer now and follow up.
        background.submit(() -> {
            try {
                String path = audio.download(mediaUrl, mediaType);
                notifier.whatsapp(phone, ideas.submit(profileName, phone, Channel.WHATSAPP, body, path).reply());
            } catch (Exception e) {
                log.warn("WhatsApp voice note failed: {}", e.getMessage());
                notifier.whatsapp(phone, "Sorry, we couldn't open your voice note. Please send it again or type your idea.");
            }
        });
        return TwiML.message("Got your voice note! We're listening to it now and will reply with your shares shortly.");
    }

    @PostMapping("/phone/incoming")
    public String phoneIncoming(@RequestParam String token) {
        guard.requireWebhook(token);
        String action = UriComponentsBuilder.fromPath("/channels/phone/recorded").queryParam("token", token).build().toUriString();
        return TwiML.record("Welcome to Set My Desk. After the beep, tell us your idea: who has the problem, what it is, " +
                "and how you would solve it. Press hash when you are done.", action);
    }

    @PostMapping("/phone/recorded")
    public String phoneRecorded(@RequestParam String token,
                                @RequestParam("From") String from,
                                @RequestParam(value = "RecordingUrl", required = false) String recordingUrl) {
        guard.requireWebhook(token);
        if (recordingUrl == null || recordingUrl.isBlank()) return TwiML.say("We did not get a recording. Please call again.");
        background.submit(() -> {
            try {
                String path = audio.download(recordingUrl + ".mp3", "audio/mpeg");
                notifier.sms(from, ideas.submit(null, from, Channel.PHONE_CALL, null, path).reply());
            } catch (Exception e) {
                log.warn("Phone recording failed: {}", e.getMessage());
            }
        });
        return TwiML.say("Thank you! We have your idea. You will get a message with your shares soon. Goodbye.");
    }
}
