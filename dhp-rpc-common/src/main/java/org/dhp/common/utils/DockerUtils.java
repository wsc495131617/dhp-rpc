package org.dhp.common.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class DockerUtils {
    private static final String ETH0 = "eth0";
    private static final String LOCAL = "127.0.0.1";

    public DockerUtils() {
    }

    public static String resolveIp() throws SocketException {
        NetworkInterface nif = NetworkInterface.getByName("eth0");
        if (nif != null) {
            Enumeration addresses = nif.getInetAddresses();

            while(addresses.hasMoreElements()) {
                InetAddress address = (InetAddress)addresses.nextElement();
                if (address instanceof Inet4Address) {
                    return address.getHostAddress();
                }
            }
        }

        throw new SocketException("未找到Docker容器中ETH0的IP");
    }

}