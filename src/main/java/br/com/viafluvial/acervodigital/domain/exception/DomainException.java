package br.com.viafluvial.acervodigital.domain.exception;

public class DomainException extends RuntimeException {

    private final String code;
    private final int httpStatusCode;

    public DomainException(String code, String message) {
        this(code, message, 400);
    }

    public DomainException(String code, String message, int httpStatusCode) {
        super(message);
        this.code = code;
        this.httpStatusCode = httpStatusCode;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
