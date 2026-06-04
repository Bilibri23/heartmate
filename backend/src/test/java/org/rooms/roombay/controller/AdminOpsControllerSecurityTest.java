package org.rooms.roombay.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class AdminOpsControllerSecurityTest {

    @Test
    void opsControllerIsMountedUnderAdminOpsApiOnly() {
        RequestMapping mapping = AdminOpsController.class.getAnnotation(RequestMapping.class);

        org.assertj.core.api.Assertions.assertThat(mapping).isNotNull();
        org.assertj.core.api.Assertions.assertThat(mapping.value()).containsExactly("/api/admin/ops");
    }

    @Test
    void opsControllerRequiresAdminRoleAtMethodSecurityLayer() {
        PreAuthorize preAuthorize = AdminOpsController.class.getAnnotation(PreAuthorize.class);

        org.assertj.core.api.Assertions.assertThat(preAuthorize).isNotNull();
        org.assertj.core.api.Assertions.assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void globalAdminSecurityRuleAlsoCoversOpsPath() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "backend/src/main/java/org/rooms/roombay/config/SecurityConfig.java"));

        org.assertj.core.api.Assertions.assertThat(source)
                .contains(".requestMatchers(\"/api/admin/**\").hasRole(\"ADMIN\")");
    }
}
