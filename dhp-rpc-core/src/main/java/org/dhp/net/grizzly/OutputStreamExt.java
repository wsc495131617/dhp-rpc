package org.dhp.net.grizzly;

import java.io.OutputStream;

/**
 * @author zhangcb
 */
public abstract class OutputStreamExt extends OutputStream {
    public abstract void writeByte(int value);

    public abstract void writeBytes(byte[] bytes);

    public abstract void writeInt(int value);

    public abstract void writeLong(long value);

    public abstract void writeDouble(double value);

    public abstract void writeShort(short value);

    public abstract void writeChar(char value);

    public abstract void writeInt(int value, int position);

    public abstract void writeLong(long value, int position);

    public abstract void writeDouble(double value, int position);

    public abstract void writeShort(short value, int position);

    public abstract void writeChar(char value, int position);
}
