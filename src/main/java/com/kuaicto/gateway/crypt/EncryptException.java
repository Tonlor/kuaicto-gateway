package com.kuaicto.gateway.crypt;

public class EncryptException extends RuntimeException {

    private static final long serialVersionUID = 7603188797924083228L;

    public EncryptException(Exception e) {
        super(e);
    }

    public EncryptException(String msg) {
        super(msg);
    }

}
