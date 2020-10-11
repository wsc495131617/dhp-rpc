package org.dhp.common.rpc;

import lombok.Builder;
import lombok.Data;

/**
 * Rpc请求通用头，其中包含一个幂等uuid，主要用于Post请求
 */
@Data
public class RpcHeader {
    //幂等
    Long uuid;
    //来源，可以自行组装
    String source;
    //来源IP
    String ip;
    //身份Token
    String authToken;

    public RpcHeader(Long uuid, String source, String ip, String authToken) {
        this.authToken = authToken;
        this.uuid = uuid;
        this.ip = ip;
        this.source = source;
    }
}
