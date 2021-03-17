package com.mooo.amksoft.amkmcauth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

    private static String getType(String type) {
        type = type.trim();
        if (type.equalsIgnoreCase("md5")) return "MD5";
        else if (type.equalsIgnoreCase("sha-512") || type.equalsIgnoreCase("sha512")) return "SHA-512";
        else if (type.equalsIgnoreCase("sha-256") || type.equalsIgnoreCase("sha256")) return "SHA-256";
        else if (type.equalsIgnoreCase("rauth"))  return  "AMKAUTH"; // Old RoyalAuth Hasher
        else if (type.equalsIgnoreCase("amkauth")) return "AMKAUTH";
        else return type;
    }

    private static String hash(String data, String type) throws NoSuchAlgorithmException {
        String rtype = Hasher.getType(type);
        if (rtype.equals("AMKAUTH")) rtype = "SHA-512";
        MessageDigest md = MessageDigest.getInstance(rtype);
        md.update(data.getBytes());
        byte byteData[] = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByteData : byteData) sb.append(Integer.toString((aByteData & 0xFF) + 0x100, 16).substring(1));
        return sb.toString();
    }

    public static String encrypt(String data, String type) throws NoSuchAlgorithmException {
        final String rtype = Hasher.getType(type);
        if (rtype.equals("AMKAUTH")) {
            for (int i = 0; i < 25; i++) data = Hasher.hash(data, rtype);
            return data;
        } else return Hasher.hash(data, rtype);
    }
}
