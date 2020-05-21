package org.dhp.core.rpc;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Constructor;

public class UnknowFailedException extends RuntimeException {
    String throwableClsName;
    String content;
    public UnknowFailedException(String clsName, String message, String content){
        super(message);
        this.throwableClsName = clsName;
        this.content = content;
    }
    
    @Override
    public synchronized Throwable getCause() {
        Class exceptionCls = null;
        try {
            exceptionCls = Class.forName(throwableClsName);
        } catch (ClassNotFoundException e) {
            return new RpcException(RpcErrorCode.UNKNOWN_EXEPTION);
        }
        //尝试反序列化
        try {
            Throwable e = (Throwable)JSON.parseObject(content, exceptionCls);
            return e;
        } catch (Exception e){
            Constructor[] constructors = exceptionCls.getConstructors();
            for(Constructor constructor : constructors){
                Class[] paramTypes = constructor.getParameterTypes();
                if(paramTypes.length == 1){
                    if(String.class == paramTypes[0]){
                        try {
                            return (Throwable) constructor.newInstance(getMessage());
                        } catch (Exception e1) {
                            return new RpcException(RpcErrorCode.UNKNOWN_EXEPTION);
                        }
                    }
                } else if(paramTypes.length == 2){
            
                }
            }
            return this;
        }
    }
}
