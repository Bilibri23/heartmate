package org.rooms.roombuddy.config;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Gmail app passwords are often pasted with spaces; JavaMail rejects them with "Could not parse mail".
 */
@Configuration
public class MailPasswordSanitizerConfig {

    @Bean
    public BeanPostProcessor mailPasswordSanitizer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) {
                if (bean instanceof MailProperties mailProperties && StringUtils.hasText(mailProperties.getPassword())) {
                    mailProperties.setPassword(mailProperties.getPassword().replaceAll("\\s+", ""));
                }
                return bean;
            }
        };
    }
}
