package com.setmydesk.channels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sends a follow-up WhatsApp/SMS after slow work (voice notes, calls) finishes. Uses Twilio's
 * Messages API when TWILIO_* variables are set; otherwise only logs, so local dev needs nothing.
 */
@Component
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);
    private final String sid;
    private final String token;
    private final String whatsappFrom;
    private final String smsFrom;
    private final RestClient http = RestClient.create();

    public Notifier(@Value("${TWILIO_ACCOUNT_SID:}") String sid,
                    @Value("${TWILIO_AUTH_TOKEN:}") String token,
                    @Value("${TWILIO_WHATSAPP_FROM:}") String whatsappFrom,
                    @Value("${TWILIO_SMS_FROM:}") String smsFrom) {
        this.sid = sid;
        this.token = token;
        this.whatsappFrom = whatsappFrom;
        this.smsFrom = smsFrom;
    }

    public void whatsapp(String toPhone, String message) {
        send(whatsappFrom.isBlank() ? null : "whatsapp:" + whatsappFrom, "whatsapp:" + toPhone, message);
    }

    public void sms(String toPhone, String message) {
        send(smsFrom.isBlank() ? null : smsFrom, toPhone, message);
    }

    private void send(String from, String to, String message) {
        if (sid.isBlank() || token.isBlank() || from == null) {
            log.info("[notify → {}] {}", mask(to), message);
            return;
        }
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("From", from);
            form.add("To", to);
            form.add("Body", message);
            String auth = Base64.getEncoder().encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));
            http.post().uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", sid)
                    .header("Authorization", "Basic " + auth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not notify {}: {}", mask(to), e.getMessage());
        }
    }

    private static String mask(String phone) {
        return phone == null || phone.length() < 4 ? "***" : "***" + phone.substring(phone.length() - 4);
    }
}
