package org.chzcb.common.test;

import org.apache.commons.codec.digest.DigestUtils;

public class TestDigestUtils {
    public static void main(String[] args) {
        String str = DigestUtils.md5Hex("dasdf");
        System.out.println(str);
        str = DigestUtils.shaHex("dasdf");
        System.out.println(str);
    }

}
