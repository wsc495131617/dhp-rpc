package org.dhp.common.rpc;

public interface IRpcHeader {
    Long getUuid();

    void setUuid(Long uuid);

    String getSource();

    void setSource(String source);

    String getIp();

    void setIp(String ip);

    String getAuthToken();

    void setAuthToken(String authToken);
}
