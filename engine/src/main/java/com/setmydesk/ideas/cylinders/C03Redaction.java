package com.setmydesk.ideas.cylinders;

import com.setmydesk.ideas.IdeaContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** 3. Ideas are public. Strip phone numbers and emails so nobody's contact details leak. */
@Component
@Order(3)
public class C03Redaction implements IdeaCylinder {
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s().-]{7,}\\d");

    public String name() { return "03-redaction"; }

    public void fire(IdeaContext ctx) {
        ctx.text = PHONE.matcher(EMAIL.matcher(ctx.text).replaceAll("[email removed]")).replaceAll("[phone removed]");
    }
}
