package org.dhp.common.spring.datasource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CheckReadOnlyDataSource extends HikariDataSource{

    static {
        java.security.Security.setProperty("networkaddress.cache.ttl", "5");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");
    }

    @Override
    public Connection getConnection() throws SQLException {

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        boolean isReadonlyDB= true;

        while (true) {
            try{
                connection = super.getConnection();
                statement = connection.createStatement();
                resultSet = statement.executeQuery("select @@innodb_read_only");
                isReadonlyDB = resultSet.next() && resultSet.getBoolean(1);
            }finally {
                try{
                    resultSet.close();
                    statement.close();

                    if(isReadonlyDB) {
                        String jdbcHostName = getHostFromUrl(getJdbcUrl());
                        int jdbcPort = getPortFromUrl(getJdbcUrl());
                        String jdbcHostAddress = getHostAddress(jdbcHostName);
                        log.warn("[ALERT] THE DATASOURCE URL TO HOST {}:{} [IP: {}:{}] IS READONLY.", jdbcHostName,jdbcPort,  jdbcHostAddress, jdbcPort);

                        jdbcHostName = getHostFromUrl(connection.getMetaData().getURL());
                        jdbcPort = getPortFromUrl(connection.getMetaData().getURL());
                        jdbcHostAddress = getHostAddress(jdbcHostName);
                        log.warn("[ALERT] THE CONNECTION OBJECT URL TO HOST {}:{} [IP: {}:{}] IS READONLY, EVICT IT AND WILL " +
                                "RETRY IN 1 SECOND.", jdbcHostName,jdbcPort,  jdbcHostAddress, jdbcPort);

                        // evict connection so that we won't get it again
                        connection.close();
                        evictConnection(connection);
                        Thread.sleep(100);

                    }else {
                        break;
                    }
                }catch (Throwable throwable){
                    //ingore the exception
                }
            }
        }

        return  connection;
    }


    final static String regexForHostAndPort = "[-.\\w]+:\\d+";
    final static Pattern hostAndPortPattern = Pattern.compile(regexForHostAndPort);

    public static String getHostAddress(String hostName) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(hostName);
        return  address.getHostAddress();
    }

    public static String getHostFromUrl(String jdbcUrl) {
        Matcher matcher = hostAndPortPattern.matcher(jdbcUrl);
        matcher.find();
        int start = matcher.start();
        int end = matcher.end();
        if(start >= 0 && end >= 0) {
            String hostAndPort = jdbcUrl.substring(start, end);
            String [] array = hostAndPort.split(":");
            if(array.length >= 2)
                return array[0];
        }
        throw new IllegalArgumentException("couldn't find pattern '" + regexForHostAndPort + "' in '" + jdbcUrl + "'");
    }

    public static  int getPortFromUrl(String jdbcUrl) {
        Matcher matcher = hostAndPortPattern.matcher(jdbcUrl);
        matcher.find();
        int start = matcher.start();
        int end = matcher.end();
        if(start >= 0 && end >= 0) {
            String hostAndPort = jdbcUrl.substring(start, end);
            String [] array = hostAndPort.split(":");
            if(array.length >= 2)
                return Integer.parseInt(array[1]);
        }
        throw new IllegalArgumentException("couldn't find pattern '" + regexForHostAndPort + "' in '" + jdbcUrl + "'");
    }

}
