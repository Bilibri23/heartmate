package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFinetuneEvalResponse {

    private int total;
    private int validJson;
    private int passedRefusal;
    private int passedNoLeak;
    private double avgLengthChars;
    private List<EvalCase> cases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvalCase {
        private String exampleId;
        private String kind;
        private String userMessage;
        private String idealAssistant;
        private String modelOutput;
        private boolean validJson;
        private boolean leakageDetected;
        private boolean refusalExpectedAndMet;
    }
}
