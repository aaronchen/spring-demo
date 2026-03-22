package cc.desuka.demo.util;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class Messages {

    private final MessageSource messageSource;

    public Messages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, Locale.getDefault());
    }
}
