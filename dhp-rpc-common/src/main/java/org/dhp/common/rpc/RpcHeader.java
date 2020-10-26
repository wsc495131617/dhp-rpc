package org.dhp.common.rpc;

/**
 * Rpc请求通用头，其中包含一个幂等uuid，主要用于Post请求
 */
public class RpcHeader implements IRpcHeader {
    //幂等
    Long uuid;
    //来源，可以自行组装
    String source;
    //来源IP
    String ip;
    //身份Token
    String authToken;

    public Long getUuid() {
        return uuid;
    }

    public void setUuid(Long uuid) {
        this.uuid = uuid;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

}
