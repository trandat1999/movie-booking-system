package com.moviebooking.modules.booking.service.impl;

import java.security.SecureRandom;

/** Sinh mã đơn/mã vé ngẫu nhiên, bỏ các ký tự dễ nhầm (0/O, 1/I/L). */
final class BookingCodes {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private BookingCodes() {}

    static String orderCode() {
        return "MB" + random(8);
    }

    static String ticketCode() {
        return "T" + random(11);
    }

    private static String random(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}
