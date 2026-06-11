package org.rooms.roombay.service;

/**
 * Optional lifecycle hooks for {@link AiAssistantService#chat} when the client streams progress.
 */
public interface AiChatProgressListener {

    default void onRetrievalStarted() {}

    default void onSourcesFound(int chunkCount) {}

    default void onGenerationStarted() {}

    default void onToken(String token) {}
}
