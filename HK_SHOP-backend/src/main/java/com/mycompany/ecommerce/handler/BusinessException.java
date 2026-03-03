package com.mycompany.ecommerce.handler;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BusinessErrorCodes code;

    public BusinessException(BusinessErrorCodes code) {
        super(code.getDescription());
        this.code = code;
    }

    public BusinessException(BusinessErrorCodes code, String message) {
        super(message);
        this.code = code;
    }
}