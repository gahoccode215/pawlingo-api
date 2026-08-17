package com.pawlingo.api.common.response;

public record ApiResponseDTO<T>(boolean success, T data, ErrorDetail error) {

    public static <T> ApiResponseDTO<T> ok(T data) {
        return new ApiResponseDTO<>(true, data, null);
    }

    public static <T> ApiResponseDTO<T> fail(String code, String message) {
        return new ApiResponseDTO<>(false, null, new ErrorDetail(code, message));
    }
}
