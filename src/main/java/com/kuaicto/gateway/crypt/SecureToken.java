package com.kuaicto.gateway.crypt;


public class SecureToken {

    private static final String SEPARATOR = "#'_'#";

    private static TokenEncryption TOKEN_ENCRYPTION = new TokenEncryption(TokenKeys.get());

    public final String raw;
    public final long active;
    public final long expiry;

    /**
     * 
     * @param raw the original string
     * @param active
     *            when the token is active (in milliseconds)
     * @param expiry
     *            when the token is expired (in milliseconds)
     */
    public SecureToken(String raw, long active, long expiry) {
        super();
        this.raw = raw;
        this.active = active;
        this.expiry = expiry;
    }

    /**
     * Create a new token based on the time when being invoked.
     * 
     * @return encrypted string
     * @throws EncryptException 
     */
    public String token() throws EncryptException {
        String unencrypted = new StringBuilder().append(this.raw).
                append(SEPARATOR).append(this.active).
                append(SEPARATOR).append(this.expiry).toString();
        String token = TOKEN_ENCRYPTION.encrypt(unencrypted);
        return token;
    }

    @Override
    public String toString() {
        return "SecureToken [raw=" + raw + ", active=" + active + ", expiry=" + expiry + "]";
    }

    public boolean isActive() {
        long now = System.currentTimeMillis();
        return now > this.active && now < this.expiry;
    }

    public static boolean isActive(SecureToken secureToken) {
        return secureToken != null && secureToken.isActive();
    }

    public static boolean isActive(String token) throws EncryptException {
        return isActive(parse(token));
    }

    public static SecureToken parse(String token) throws EncryptException {
        String decrypt = TOKEN_ENCRYPTION.decrypt(token);
        String[] arr = decrypt.split(SEPARATOR);
        if (arr != null && arr.length == 3) {
            String raw = arr[0];
            long active = Long.valueOf(arr[1]);
            long expiry = Long.valueOf(arr[2]);

            return new SecureToken(raw, active, expiry);
        }
        return null;
    }
    
    public static void setTokenKeys(String... keys) {
        TOKEN_ENCRYPTION = new TokenEncryption(keys);
    }
}
