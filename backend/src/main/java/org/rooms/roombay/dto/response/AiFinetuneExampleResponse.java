package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.AiFinetuneExample;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFinetuneExampleResponse {
    private UUID id;
    private AiFinetuneExample.Kind kind;
    private AiFinetuneExample.Persona persona;
    private List<String> tags;
    private String systemPrompt;
    private String userMessage;
    private String sanitizedContext;
    private String idealAssistant;
    private String responseFormat;
    private AiFinetuneExample.Source source;
    private String notes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
