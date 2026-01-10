package com.liskovsoft.googlecommon.common.helpers;

import com.liskovsoft.sharedutils.helpers.Helpers;

import java.util.Random;

import javax.annotation.Nonnull;

 
public final class RandomStringFromAlphabetGenerator {
    private static final String CONTENT_PLAYBACK_NONCE_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private RandomStringFromAlphabetGenerator() {
         
    }

     
    @Nonnull
    public static String generate(
            final String alphabet,
            final int length,
            final Random random) {
        final StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return stringBuilder.toString();
    }

     
    @Nonnull
    public static String generate(
            final int length) {
        return generate(CONTENT_PLAYBACK_NONCE_ALPHABET, length, Helpers.getRandom());
    }
}
