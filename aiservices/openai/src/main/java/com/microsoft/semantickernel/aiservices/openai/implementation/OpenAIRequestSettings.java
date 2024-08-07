// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.aiservices.openai.implementation;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.policy.UserAgentPolicy;
import com.azure.core.http.rest.RequestOptions;
import com.azure.core.util.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;

/**
 * Provides Http request settings for OpenAI requests.
 */
public final class OpenAIRequestSettings {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
        OpenAIRequestSettings.class);

    private static final String SEMANTIC_KERNEL_VERSION_PROPERTY_NAME = "semantic-kernel.version";
    private static final String SEMANTIC_KERNEL_VERSION_PROPERTIES_FILE = "semantic-kernel-version.properties";

    private static final String useragent;
    private static final String header;

    public static final String SEMANTIC_KERNEL_DISABLE_USERAGENT_PROPERTY = "semantic-kernel.useragent-disable";

    private static final boolean disabled;

    static {
        disabled = isDisabled();
        String version = loadVersion();
        useragent = "semantic-kernel-java/" + version;
        header = "java/" + version;
    }

    private static boolean isDisabled() {
        return Boolean.parseBoolean(
            System.getProperty(SEMANTIC_KERNEL_DISABLE_USERAGENT_PROPERTY, "false"));
    }

    private static String loadVersion() {

        String version = "unknown";

        try (InputStream settingsFile = OpenAIRequestSettings.class.getResourceAsStream(
            SEMANTIC_KERNEL_VERSION_PROPERTIES_FILE)) {

            Properties props = new Properties();
            props.load(settingsFile);
            if (props.containsKey(SEMANTIC_KERNEL_VERSION_PROPERTY_NAME)) {
                String skVersion = props.getProperty(SEMANTIC_KERNEL_VERSION_PROPERTY_NAME);
                if (skVersion != null && !skVersion.isEmpty()) {
                    return skVersion;
                }
            }
        } catch (IOException e) {
            //Ignore
            LOGGER.trace("Failed to load Semantic Kernel version from properties file", e);
        }
        return version;
    }

    /**
     * Get the HTTP request options for OpenAI requests.
     *
     * @return The request options
     */
    public static RequestOptions getRequestOptions() {
        RequestOptions requestOptions = new RequestOptions();

        if (disabled) {
            return requestOptions;
        }

        return requestOptions
            .setHeader(HttpHeaderName.fromString("Semantic-Kernel-Version"), header)
            .setContext(new Context(UserAgentPolicy.APPEND_USER_AGENT_CONTEXT_KEY, useragent));
    }
}
