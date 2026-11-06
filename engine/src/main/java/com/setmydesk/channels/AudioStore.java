package com.setmydesk.channels;

import com.setmydesk.core.EngineProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/** Saves voice notes to disk under random names. Only the server ever chooses the path. */
@Component
public class AudioStore {

    private static final Set<String> EXTENSIONS = Set.of("webm", "ogg", "oga", "mp3", "m4a", "mp4", "wav", "aac", "amr", "3gp");
    private final Path dir;
    private final RestClient http = RestClient.create();

    public AudioStore(EngineProperties props) throws IOException {
        this.dir = Path.of(props.audioDir()).toAbsolutePath();
        Files.createDirectories(dir);
    }

    public String save(InputStream in, String originalName) throws IOException {
        Path target = dir.resolve(UUID.randomUUID() + "." + extension(originalName));
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    /** Downloads media from a messaging provider (WhatsApp/phone recording). HTTPS only. */
    public String download(String url, String contentType) throws IOException {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IOException("Refusing non-HTTPS media URL");
        byte[] bytes = http.get().uri(uri).retrieve().body(byte[].class);
        if (bytes == null) throw new IOException("Empty media from " + uri.getHost());
        String ext = contentType != null && contentType.contains("/") ? contentType.substring(contentType.indexOf('/') + 1) : "ogg";
        return save(new java.io.ByteArrayInputStream(bytes), "media." + ext.replaceAll(";.*", ""));
    }

    static String extension(String name) {
        if (name == null || !name.contains(".")) return "webm";
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (ext.equals("mpeg")) ext = "mp3";
        return EXTENSIONS.contains(ext) ? ext : "webm";
    }
}
