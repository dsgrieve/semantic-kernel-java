// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides the base exception from which all Semantic Kernel exceptions derive.
 */
public class SKException extends RuntimeException {

    /**
     * Initializes a new instance of the {@code SKException} class with a default message.
     */
    protected SKException() {
        super();
    }

    /**
     * Initializes a new instance of the {@code SKException} class with its message set to
     * {@code message}.
     *
     * @param message A string that describes the error.
     */
    public SKException(@Nullable String message) {
        super(message);
    }

    /**
     * Initializes a new instance of the {@code SKException} class with its message set to
     * {@code message}.
     *
     * @param message A string that describes the error.
     * @param cause   The exception that is the cause of the current exception.
     */
    public SKException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    /**
     * Initializes a new instance of the {@code SKException} class with its
     * message set to {@code null} and the cause set to {@code e}.
     * 
     * @param cause The exception that is the cause of the current exception.
     */
    public SKException(Throwable cause) {
        super(cause);
    }

    /**
     * Forms an unchecked exception, if the exception is already an SK exception, it will be
     * unwrapped and the cause extracted.
     *
     * @param message The message to be displayed
     * @param cause   The exception that is the cause of the current exception.
     * @return An unchecked exception
     */
    public static SKException build(
        String message,
        @Nullable Exception cause) {

        if (cause == null) {
            return new SKException(message);
        }

        Throwable wrappedCause = cause.getCause();

        if ((cause instanceof SKCheckedException || cause instanceof SKException)
            && wrappedCause != null) {
            return new SKException(message, wrappedCause);
        } else {
            return new SKException(message, cause);
        }
    }

    /**
     * Translate the error code into a default message format.
     *
     * @param errorMessage The error message from an error code
     * @param message      The message from the code which throws the exception
     * @return A formatted message
     */
    protected static String formatDefaultMessage(
        @Nonnull String errorMessage, @Nullable String message) {
        return String.format("%s: %s", errorMessage, message);
    }
}
