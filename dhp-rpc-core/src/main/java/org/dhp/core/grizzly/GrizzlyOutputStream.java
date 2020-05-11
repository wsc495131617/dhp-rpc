package org.dhp.core.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.HeapBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

import java.io.IOException;

public class GrizzlyOutputStream extends OutputStreamExt {

    /**
     * 最小包的大小
     */
    private final static int NEW_BUFFER_SIZE_AT_LEAST = 128;

    protected Buffer currentBuffer;

    public GrizzlyOutputStream() {
        this(null);
    }

    public GrizzlyOutputStream(Buffer buffer) {
        this.currentBuffer = buffer;
    }

    public void skip(int len) {
        ensureCapacity(len);
        this.currentBuffer.position(this.currentBuffer.position() + len);
    }

    private void ensureCapacity(final int len) {
        if (currentBuffer == null) {
            currentBuffer = MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(Math.max(NEW_BUFFER_SIZE_AT_LEAST, len));
        } else if (currentBuffer.remaining() < len) {
            if (currentBuffer.capacity() - currentBuffer.limit() > len) {
                currentBuffer.limit(len - currentBuffer.remaining() + currentBuffer.limit());
            } else {
                currentBuffer = MemoryManager.DEFAULT_MEMORY_MANAGER.reallocate((HeapBuffer) currentBuffer,
                        Math.max(currentBuffer.capacity() + len, (currentBuffer.capacity() * 3) / 2 + 1));
            }
        }
    }

    public void writeBuffer(Buffer buffer) {
        ensureCapacity(buffer.remaining());
        currentBuffer.put(buffer);
    }

    @Override
    public void writeShort(short i) {
        ensureCapacity(2);
        currentBuffer.putShort(i);
    }

    @Override
    public void writeInt(int i) {
        ensureCapacity(4);
        currentBuffer.putInt(i);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureCapacity(len);
        currentBuffer.put(b, off, len);
    }

    public void writeBytes(byte[] bytes) {
        int len = bytes.length;
        ensureCapacity(len);
        currentBuffer.put(bytes, 0, len);
    }

    public Buffer getBuffer() {
        if (currentBuffer == null) {
            return HeapBuffer.wrap(new byte[0]);
        }
        if (currentBuffer.position() > 0) {
            currentBuffer.flip();
        }
        return currentBuffer;
    }

    @Override
    public void write(int b) throws IOException {
        ensureCapacity(1);
        currentBuffer.put((byte) b);
    }

    public void writeByte(int b) {
        ensureCapacity(1);
        currentBuffer.put((byte) b);
    }

    @Override
    public void writeLong(long value) {
        ensureCapacity(8);
        currentBuffer.putLong(value);
    }

    @Override
    public void writeDouble(double value) {
        ensureCapacity(8);
        currentBuffer.putDouble(value);
    }

    @Override
    public void writeChar(char value) {
        ensureCapacity(2);
        currentBuffer.putChar(value);
    }

    @Override
    public void writeInt(int value, int position) {
        ensureCapacity(4);
        currentBuffer.putInt(position, value);
    }

    @Override
    public void writeLong(long value, int position) {
        ensureCapacity(8);
        currentBuffer.putLong(position, value);
    }

    @Override
    public void writeDouble(double value, int position) {
        ensureCapacity(8);
        currentBuffer.putDouble(position, value);
    }

    @Override
    public void writeShort(short value, int position) {
        ensureCapacity(2);
        currentBuffer.putShort(position, value);
    }

    @Override
    public void writeChar(char value, int position) {
        ensureCapacity(2);
        currentBuffer.putChar(position, value);
    }

    @Override
    public void flush() throws IOException {
        if (currentBuffer != null && currentBuffer.position() > 0) {
            currentBuffer.flip();
        }
    }

    @Override
    public void close() throws IOException {
        if (currentBuffer != null) {
            this.currentBuffer.tryDispose();
            this.currentBuffer = null;
        }
    }

}
