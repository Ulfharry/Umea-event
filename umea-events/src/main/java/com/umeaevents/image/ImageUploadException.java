package com.umeaevents.image;

/** Thrown when an image can't be stored (storage unavailable/misconfigured/rejected). */
public class ImageUploadException extends RuntimeException {
    public ImageUploadException(String message) {
        super(message);
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
