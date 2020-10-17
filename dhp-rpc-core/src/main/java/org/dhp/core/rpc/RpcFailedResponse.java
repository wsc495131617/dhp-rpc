package org.dhp.core.rpc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author zhangcb
 */
@Slf4j
@Data
public class RpcFailedResponse {
    String clsName;
    String message;

    byte[] throwableContent;

    public void packThrowable(Throwable throwable) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(byteArrayOutputStream);
            oo.writeObject(throwable);
            oo.flush();
            oo.close();
            this.throwableContent = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
        }
    }

    public Throwable unpackThrowable() {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(throwableContent);
            ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
            try {
                Throwable ex = (Throwable) ois.readObject();
                return ex;
            } catch (ClassNotFoundException e) {
                log.error("ClassNotFoundException: {}", e.getMessage());
                return null;
            } finally {
                ois.close();
            }
        } catch (Exception e){
        }
        return null;
    }

}
