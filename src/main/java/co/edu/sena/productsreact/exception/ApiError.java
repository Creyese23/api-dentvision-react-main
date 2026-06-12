package co.edu.sena.productsreact.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> fields
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, LocalDateTime.now(), null);
    }

    public static ApiError ofValidation(int status, String message, String path, Map<String, String> fields) {
        return new ApiError(status, "Bad Request", message, path, LocalDateTime.now(), fields);
    }
}
