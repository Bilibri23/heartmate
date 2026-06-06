package org.rooms.roombay.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadControllerSecurityTest {

    @Test
    void verificationUploadsAreStudentOrAdminOnly() throws Exception {
        PreAuthorize studentId = FileUploadController.class
                .getMethod("uploadStudentIdPhoto", org.springframework.web.multipart.MultipartFile.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize verificationDocument = FileUploadController.class
                .getMethod("uploadVerificationDocument", org.springframework.web.multipart.MultipartFile.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(studentId.value()).isEqualTo("hasAnyRole('STUDENT', 'ADMIN')");
        assertThat(verificationDocument.value()).isEqualTo("hasAnyRole('STUDENT', 'LANDLORD', 'ADMIN')");
    }
}
