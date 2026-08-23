package com.pawlingo.api.common.response;

public record ApiResponseDTO<T>(boolean success, T data, ErrorDetail error, PageMeta meta) {

    public static <T> ApiResponseDTO<T> ok(T data) {
        return new ApiResponseDTO<>(true, data, null, null);
    }

    public static <T> ApiResponseDTO<T> ok(T data, PageMeta meta) {
        return new ApiResponseDTO<>(true, data, null, meta);
    }

    public static <T> ApiResponseDTO<T> fail(String code, String message) {
        return new ApiResponseDTO<>(false, null, new ErrorDetail(code, message), null);
    }
}
