package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.AiFinetuneExample;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFinetuneExampleRequest {

    @NotNull
    private AiFinetuneExample.Kind kind;

    @NotNull
    private AiFinetuneExample.Persona persona;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @NotBlank
    @Size(max = 6000)
    private String systemPrompt;

    @NotBlank
    @Size(max = 4000)
    private String userMessage;

    @Size(max = 8000)
    private String sanitizedContext;

    @NotBlank
    @Size(max = 4000)
    private String idealAssistant;

    /** "text" or "json_object" */
    @Size(max = 20)
    @Builder.Default
    private String responseFormat = "text";

    @Size(max = 1000)
    private String notes;
}
