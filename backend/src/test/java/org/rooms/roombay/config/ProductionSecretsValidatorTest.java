package org.rooms.roombay.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecretsValidatorTest {

    @Test
    void prodAllowsAdminSeedDuringRecoveryByDefault() {
        MockEnvironment env = baseProdEnvironment()
                .withProperty("app.admin.seed.enabled", "true");

        assertThatCode(() -> new ProductionSecretsValidator(env).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void prodRejectsAdminSeedWhenStrictFlagIsEnabled() {
        MockEnvironment env = baseProdEnvironment()
                .withProperty("app.admin.seed.enabled", "true")
                .withProperty("app.admin.seed.fail-prod-enabled", "true");

        assertThatThrownBy(() -> new ProductionSecretsValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ADMIN_SEED_ENABLED must be false");
    }

    @Test
    void prodRequiresManagedRedisWhenRedisIsRequired() {
        MockEnvironment env = baseProdEnvironment()
                .withProperty("ratelimit.redis.required", "true")
                .withProperty("spring.data.redis.host", "localhost");

        assertThatThrownBy(() -> new ProductionSecretsValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production Redis rate limiting requires REDIS_HOST");
    }

    @Test
    void devAllowsAdminSeedAndLocalRedis() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.profiles.active", "dev")
                .withProperty("app.admin.seed.enabled", "true")
                .withProperty("ratelimit.redis.required", "true")
                .withProperty("spring.data.redis.host", "localhost");
        env.setActiveProfiles("dev");

        assertThatCode(() -> new ProductionSecretsValidator(env).validate())
                .doesNotThrowAnyException();
    }

    private static MockEnvironment baseProdEnvironment() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.jwt.secret", "12345678901234567890123456789012")
                .withProperty("cloudinary.cloud-name", "cloud")
                .withProperty("cloudinary.api-key", "key")
                .withProperty("cloudinary.api-secret", "secret")
                .withProperty("cors.allowed-origins", "https://www.roombay.app")
                .withProperty("spring.datasource.url", "jdbc:postgresql://postgres/railway")
                .withProperty("spring.datasource.username", "user")
                .withProperty("spring.datasource.password", "pass")
                .withProperty("spring.flyway.user", "user")
                .withProperty("spring.flyway.password", "pass")
                .withProperty("spring.mail.username", "mail")
                .withProperty("spring.mail.password", "pass")
                .withProperty("app.admin.seed.enabled", "false")
                .withProperty("spring.data.redis.host", "redis.internal");
        env.setActiveProfiles("prod");
        return env;
    }
}
