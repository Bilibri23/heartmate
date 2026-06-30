package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Body for {@code POST /api/ai/actions/execute}: a landlord/admin write action the user explicitly
 * confirmed via the chat confirm-button. The server re-validates role and ownership before running it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiActionExecuteRequest {

    @NotBlank(message = "tool is required")
    private String tool;

    /** Action parameters (e.g. targetId, reason, verificationType). */
    private Map<String, Object> params;
}
