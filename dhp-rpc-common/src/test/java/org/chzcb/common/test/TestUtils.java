package org.chzcb.common.test;

import org.dhp.common.utils.LocalIPUtils;

public class TestUtils {
    public static void main(String[] args) {
        System.out.println(LocalIPUtils.hostName());
        System.out.println(LocalIPUtils.resolveIp());
    }
}
