package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.net.BufferMessage;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.memory.PooledMemoryManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

@Slf4j
public class MessageDecoder {

    //HeapMemoryManager 因为需要线程池支持缓存，所以没用grizzly框架就不能用HeapMemoryManager
    public static MemoryManager memoryManager = new PooledMemoryManager(){
        @Override
        protected Object createJmxManagementObject() {
            return new MemoryManagerJmxObject(this);
        }
    };

    protected ByteBuffer buffer;

    public MessageDecoder(int bufferSize) {
        buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    protected int position;
    protected int limit;


    public boolean read(SocketChannel socket, List<BufferMessage> list) {
        try {
            limit = socket.read(buffer);
            //如果读取为-1，说明已经断开
            if (limit == -1) {
                return false;
            }
            if (limit == 0) {
                return true;
            }
            Buffer msgBuf = null;
            //写满了缓存
            while (true) {
                if (msgBuf == null) {
                    //获取头
                    int len = buffer.getInt(position);
                    msgBuf = memoryManager.allocate(len);
                }
                int remaining = msgBuf.remaining();
                //msgBuf写完整
                if (remaining <= limit - position) {
                    msgBuf.put(buffer, position, remaining);
                    position += remaining;
                    buffer.position(position);

                    BufferMessage msg = new BufferMessage(msgBuf);
                    list.add(msg);
                    //重置，粘包处理
                    msgBuf.tryDispose();
                    msgBuf = null;
                    //如果下一个包头存在
                    if (limit - position > 4) {
                        continue;
                    } else {
                        position = 0;
                        buffer.position(0);
                    }
                } else {
                    msgBuf.put(buffer, position, limit - position);
                    //buffer全部写完
                    buffer.position(0);
                    position = 0;
                }

                //继续写
                limit = socket.read(buffer);
                if (limit <= 0) {
                    break;
                }
            }
            return true;
        } catch (IOException e) {
            log.warn("Decoder error {}", e.getMessage());
            return false;
        }
    }

    public void destroy() {
        buffer.clear();
        buffer = null;
    }
}
