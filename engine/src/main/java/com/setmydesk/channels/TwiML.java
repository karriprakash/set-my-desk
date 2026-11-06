package com.setmydesk.channels;

/** Minimal TwiML builder for WhatsApp/SMS and voice responses. */
final class TwiML {
    private TwiML() {
    }

    static String message(String text) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Message>" + esc(text) + "</Message></Response>";
    }

    static String empty() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response/>";
    }

    static String record(String greeting, String actionUrl) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response>"
                + "<Say voice=\"Polly.Aditi\">" + esc(greeting) + "</Say>"
                + "<Record action=\"" + esc(actionUrl) + "\" method=\"POST\" maxLength=\"180\" playBeep=\"true\" finishOnKey=\"#\"/>"
                + "<Say>We did not hear anything. Please call again. Goodbye.</Say>"
                + "</Response>";
    }

    static String say(String text) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say voice=\"Polly.Aditi\">" + esc(text) + "</Say><Hangup/></Response>";
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
