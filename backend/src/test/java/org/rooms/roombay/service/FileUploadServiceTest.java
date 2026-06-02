package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadServiceTest {

    private final FileUploadService service = new FileUploadService(null);

    @Test
    void imageValidationAcceptsMatchingJpegSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "validateFile", file, 10L * 1024 * 1024))
                .doesNotThrowAnyException();
    }

    @Test
    void imageValidationRejectsMismatchedSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "not an image".getBytes(StandardCharsets.US_ASCII)
        );

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateFile", file, 10L * 1024 * 1024))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void videoValidationRejectsMismatchedSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.mp4",
                "video/mp4",
                "not a video".getBytes(StandardCharsets.US_ASCII)
        );

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateVideoFile", file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }
}
