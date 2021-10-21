package org.dhp.common.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class LocalIPUtils {
    public static final String[] InterfaceNames = new String[] {"en*","eth*","lo*"};
    private static final String LOCAL = "127.0.0.1";

    public static String hostName() {
        try {
            return Inet4Address.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    public static String resolveIp()  {
        try {
            String[] ips = new String[InterfaceNames.length];
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while(nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                for(int i=0;i<InterfaceNames.length;i++) {
                    String name = InterfaceNames[i];
                    if(ips[i] == null && nif.getName().matches(name)) {
                        Enumeration addresses = nif.getInetAddresses();
                        while(addresses.hasMoreElements()) {
                            InetAddress address = (InetAddress)addresses.nextElement();
                            if (address instanceof Inet4Address) {
                                ips[i] =  address.getHostAddress();
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

}
