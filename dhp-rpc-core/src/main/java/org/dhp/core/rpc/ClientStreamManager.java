package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.JacksonUtil;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.spring.FrameworkException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端的StreamManager，用于管理所有的流
 *
 * @author zhangcb
 */
@Slf4j
public class ClientStreamManager {

    static Map<Integer, Stream> handlerMap = new ConcurrentHashMap<>();
    static Map<Integer, Message> streamMessages = new ConcurrentHashMap<>();

    public Throwable dealThrowable(Message message) {
        RpcFailedResponse failedResponse = ProtostuffUtils.deserialize(message.getData(), RpcFailedResponse.class);
        if (RpcException.class.getName().equalsIgnoreCase(failedResponse.getClsName())) {
            List<String> list = JacksonUtil.json2Bean(failedResponse.getMessage(), List.class);
            return new RpcException(RpcErrorCode.valueOf(list.remove(0)));
        } else {
            log.warn("throwable: {},{}", failedResponse.getClsName(), failedResponse.getMessage());
            return failedResponse.unpackThrowable();
        }
    }

    /**
     * 设置流
     *
     * @param message
     * @param stream
     */
    public void setStream(Message message, Stream stream) {
        handlerMap.put(message.getId(), stream);
        streamMessages.put(message.getId(), message);
    }

    public ClientStreamManager() {

    }

    /**
     * Deal message from rpc server
     *
     * @param message
     */
    public void handleMessage(Message message) {
        Stream handler = handlerMap.get(message.getId());
        MessageStatus status = message.getStatus();
        switch (status) {
            case Canceled:
                clearId(message.getId());
                break;
            case Completed:
                try {
                    handler.onNext(message);
                    handler.onCompleted();
                } catch (Throwable e) {
                    log.warn(e.getMessage(), e);
                } finally {
                    handlerMap.remove(message.getId());
                    streamMessages.remove(message.getId());
                }
                break;
            case Updating:
                handler.onNext(message);
                break;
            case Failed:
                //需要分为no command还是别的
                handler.onFailed(dealThrowable(message));
                break;
            default:
                throw new FrameworkException("Sending MessageStatus can't response");
        }
    }


    public void clearId(Integer id) {
        Stream stream = handlerMap.remove(id);
        if (stream != null) {
            stream.onCanceled();
        }
        Message message = streamMessages.remove(id);
        if (message != null) {
            message.close();
        }
    }
}
