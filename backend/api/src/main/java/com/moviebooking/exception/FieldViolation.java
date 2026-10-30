package com.moviebooking.exception;

/** Một lỗi validate trên một trường của request. */
public record FieldViolation(String field, String message) {}
