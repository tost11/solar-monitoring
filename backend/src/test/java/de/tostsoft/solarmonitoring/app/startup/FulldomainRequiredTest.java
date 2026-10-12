package de.tostsoft.solarmonitoring.app.startup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FulldomainRequiredTest {

    @Test
    void failsToStartWithoutFulldomain() {
        Throwable thrown = catchThrowable(() -> {
            try (var ctx = new AnnotationConfigApplicationContext()) {
                ctx.register(FulldomainConfig.class);
                ctx.refresh();
            }
        });

        assertThat(thrown).isNotNull();
        Throwable cause = thrown;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertThat(cause.getMessage()).contains("fulldomain");
    }

    @Configuration
    static class FulldomainConfig {
        @Value("${fulldomain}")
        String fulldomain;

        @Bean
        static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
