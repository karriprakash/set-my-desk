package com.setmydesk.web;

import com.setmydesk.channels.AudioStore;
import com.setmydesk.channels.WebhookGuard;
import com.setmydesk.ideas.Channel;
import com.setmydesk.ideas.Idea;
import com.setmydesk.ideas.IdeaRepository;
import com.setmydesk.ideas.IdeaService;
import com.setmydesk.ideas.IdeaStatus;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/ideas")
public class IdeaController {

    private final IdeaService service;
    private final IdeaRepository ideas;
    private final AudioStore audio;
    private final WebhookGuard guard;

    public IdeaController(IdeaService service, IdeaRepository ideas, AudioStore audio, WebhookGuard guard) {
        this.service = service;
        this.ideas = ideas;
        this.audio = audio;
        this.guard = guard;
    }

    /** Typed idea from the website or the mobile app. */
    @PostMapping
    public IdeaService.Outcome submit(@Valid @RequestBody Dtos.SubmitIdea body) {
        Channel channel = "APP".equalsIgnoreCase(body.channel()) ? Channel.APP : Channel.WEB;
        return service.submit(body.name(), body.phone(), channel, body.text(), null);
    }

    /** Voice note recorded in the browser or the app. Optional text is used if present. */
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IdeaService.Outcome voice(@RequestPart("audio") MultipartFile file,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String text) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Audio file is empty");
        String type = file.getContentType();
        if (type != null && !type.startsWith("audio/") && !type.startsWith("video/") && !type.equals("application/octet-stream")) {
            throw new IllegalArgumentException("Please upload an audio file");
        }
        String path = audio.save(file.getInputStream(), file.getOriginalFilename());
        return service.submit(name, phone, Channel.VOICE_NOTE, text, path);
    }

    @GetMapping("/{id}")
    public Dtos.IdeaView get(@PathVariable Long id) {
        return Dtos.IdeaView.of(find(id));
    }

    // ---- Volunteer transcription desk (needs X-Volunteer-Token) ----

    @GetMapping("/pending-transcription")
    public List<Dtos.PendingVoiceNote> pending(@RequestHeader("X-Volunteer-Token") String token) {
        guard.requireVolunteer(token);
        return ideas.findByStatusOrderByCreatedAtAsc(IdeaStatus.PENDING_TRANSCRIPTION).stream()
                .map(i -> new Dtos.PendingVoiceNote(i.getId(), i.getContributor().getDisplayName(), i.getChannel().name(),
                        "/api/ideas/" + i.getId() + "/audio", i.getCreatedAt()))
                .toList();
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable Long id, @RequestHeader("X-Volunteer-Token") String token) {
        guard.requireVolunteer(token);
        Idea idea = find(id);
        if (idea.getAudioPath() == null) throw new NoSuchElementException("No audio for idea " + id);
        FileSystemResource file = new FileSystemResource(idea.getAudioPath());
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(file);
    }

    @PostMapping("/{id}/transcript")
    public IdeaService.Outcome transcript(@PathVariable Long id, @RequestHeader("X-Volunteer-Token") String token,
                                          @Valid @RequestBody Dtos.Transcript body) {
        guard.requireVolunteer(token);
        return service.completeTranscription(id, body.text());
    }

    private Idea find(Long id) {
        return ideas.findById(id).orElseThrow(() -> new NoSuchElementException("No idea " + id));
    }
}
