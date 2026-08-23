package com.tendermatch.dto.common;

import java.time.ZonedDateTime;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private ZonedDateTime timestamp = ZonedDateTime.now();

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data, ZonedDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp != null ? timestamp : ZonedDateTime.now();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, ZonedDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Success");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, ZonedDateTime.now());
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private boolean success;
        private String message;
        private T data;
        private ZonedDateTime timestamp = ZonedDateTime.now();

        public Builder<T> success(boolean success) { this.success = success; return this; }
        public Builder<T> message(String message) { this.message = message; return this; }
        public Builder<T> data(T data) { this.data = data; return this; }
        public Builder<T> timestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; return this; }

        public ApiResponse<T> build() {
            return new ApiResponse<>(success, message, data, timestamp);
        }
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }
}
