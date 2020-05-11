package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.utils.ProtostuffUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Slf4j
public class MethodDispatchUtils {

    public static byte[] dealFailed(ServerCommand command, Throwable e) {
        String throwableClassName = e.getClass().getName();
        String throwableMessage = e.getMessage();
        String throwableContent = e.toString();
        RpcFailedResponse response = RpcFailedResponse.builder().clsName(throwableClassName)
                .message(throwableMessage)
                .content(throwableContent)
                .build();
        return ProtostuffUtils.serialize(RpcFailedResponse.class, response);
    }

    public static byte[] dealResult(ServerCommand command, Object result) {
        try {
            if(command.getType() == MethodType.Default) {
                return ProtostuffUtils.serialize((Class) command.getMethod().getReturnType(), result);
            } else if(command.getType() == MethodType.Future){
                ParameterizedType type = (ParameterizedType)command.getMethod().getGenericReturnType();
                Class clas = (Class)type.getActualTypeArguments()[0];
                return ProtostuffUtils.serialize(clas, result);
            } else if(command.getType() == MethodType.Stream){
                Type[] paramTypes = command.getMethod().getGenericParameterTypes();
                if(paramTypes[0] instanceof ParameterizedType){
                    ParameterizedType pType = (ParameterizedType)paramTypes[0];
                    return ProtostuffUtils.serialize((Class) pType.getActualTypeArguments()[0], result);
                } else {
                    ParameterizedType pType = (ParameterizedType)paramTypes[1];
                    return ProtostuffUtils.serialize((Class) pType.getActualTypeArguments()[0], result);
                }
            }
        } catch (Throwable e){
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
