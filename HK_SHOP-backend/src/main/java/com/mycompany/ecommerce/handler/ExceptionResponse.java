package com.mycompany.ecommerce.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExceptionResponse {

    // Meta
    private Instant timestamp;
    private Integer status;
    private String path;

    // Business info (stable contract for frontend)
    private Integer businessErrorCode;
    private String businessErrorDescription;

    // Display message (user-friendly)
    private String message;

    // Technical/debug (optional)
    private String error;

    // Validation details
    private Set<String> validationErrors;
    private Map<String, String> fieldErrors;
}