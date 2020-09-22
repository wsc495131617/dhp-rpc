package org.dhp.common.servlet;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

@Slf4j
public class RequestIPUtils {

    static Pattern IP_PATTERN = Pattern.compile("/^((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)$/");

    /**
     *
     * @param ip
     * @return
     */
    public static boolean isInetAddress(String ip) {
        if(ip == null) {
            return false;
        }
        try {
            if (IP_PATTERN.matcher(ip).find()) {
                String[] arr = ip.split("\\.");
                for (String str : arr) {
                    Integer value = Integer.parseInt(str);
                    if (value > 255) {
                        return false;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            log.warn("Ip Address {} check failed!", ip);
            return false;
        }
        return false;
    }

    /**
     * 默认通过X-Real-IP获取
     * @param request
     * @return
     */
    public static String getRemoteIp(HttpServletRequest request) {
        return getRemoteIp(request, "X-Real-IP");
    }

    /**
     * 通过X-Forwarded-For正常获取，这里依赖CDN的X-Forwarded-For重写
     * 同样的道理，Nginx因为不能重写X-Forwarded-For，只能增加，因此可以自定义Header字段，让Nginx转发进来
     * @param request
     * @return
     */
    public static String getRemoteIp(HttpServletRequest request, String realIpHeader) {
        //先获取X-Forwarded-For
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        if (!StringUtils.isNotBlank(ip) && isInetAddress(ip)) {
            return ip;
        } else {
            ip = request.getHeader(realIpHeader);
            return !StringUtils.isNotBlank(ip) && isInetAddress(ip) ? ip : request.getRemoteAddr();
        }
    }
}
