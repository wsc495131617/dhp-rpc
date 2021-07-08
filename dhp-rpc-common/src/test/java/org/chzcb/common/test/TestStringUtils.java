package org.chzcb.common.test;

import org.dhp.common.utils.StringUtils;

public class TestStringUtils {
    public static void main(String[] args) {
        String str = "org.Test";
        System.out.println(str+"=>"+StringUtils.simplePackage(str));
        str = "org.Test:test";
        System.out.println(str+"=>"+StringUtils.simplePackage(str));
        str = "Test";
        System.out.println(str+"=>"+StringUtils.simplePackage(str));
    }
}
