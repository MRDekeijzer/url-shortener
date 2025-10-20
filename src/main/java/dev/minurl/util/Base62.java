package dev.minurl.util;

import java.math.BigInteger;

public final class Base62 {
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final BigInteger BASE = BigInteger.valueOf(62);

    private Base62() {
    }

    public static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }
        BigInteger value = new BigInteger(1, input);
        if (value.equals(BigInteger.ZERO)) {
            return "0";
        }
        StringBuilder builder = new StringBuilder();
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(BASE);
            builder.append(ALPHABET[divRem[1].intValue()]);
            value = divRem[0];
        }
        return builder.reverse().toString();
    }
}
