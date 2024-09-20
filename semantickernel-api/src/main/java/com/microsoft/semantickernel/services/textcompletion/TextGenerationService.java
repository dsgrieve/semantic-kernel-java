// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.services.textcompletion;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.implementation.ServiceLoadUtil;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.services.StreamingTextContent;
import com.microsoft.semantickernel.services.TextAIService;
import com.microsoft.semantickernel.services.openai.OpenAiServiceBuilder;
import java.util.List;
import javax.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for text completion services
 */
public interface TextGenerationService extends TextAIService {

    /**
     * Get the builder for the TextGenerationService
     *
     * @return The builder
     */
    static Builder builder() {
        return ServiceLoadUtil.findServiceLoader(Builder.class,
            "com.microsoft.semantickernel.aiservices.openai.textcompletion.OpenAITextGenerationService$Builder")
            .get();
    }

    /**
     * Creates a completion for the prompt and settings.
     *
     * @param prompt            The prompt to complete.
     * @param executionSettings Request settings for the completion API
     * @param kernel            The {@code Kernel} containing services, plugins, and other state for
     *                          use throughout the operation.
     * @return Text generated by the remote model
     */
    Mono<List<TextContent>> getTextContentsAsync(
        String prompt,
        @Nullable PromptExecutionSettings executionSettings,
        @Nullable Kernel kernel);

    /**
     * Get streaming results for the prompt using the specified execution settings. Each modality
     * may support for different types of streaming contents.
     *
     * @param prompt            The prompt to complete.
     * @param executionSettings The AI execution settings (optional).
     * @param kernel            The {@code Kernel} containing services, plugins, and other state for
     *                          use throughout the operation.
     * @return Streaming list of different completion streaming string updates generated by the
     * remote model
     */
    Flux<StreamingTextContent> getStreamingTextContentsAsync(
        String prompt,
        @Nullable PromptExecutionSettings executionSettings,
        @Nullable Kernel kernel);

    /**
     * Builder for a TextGenerationService
     */
    abstract class Builder
        extends OpenAiServiceBuilder<OpenAIAsyncClient, TextGenerationService, Builder> {
    }
}
