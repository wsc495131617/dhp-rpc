package org.dhp.core;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

public class TestByteBuffer {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put(new byte[]{1,2,3,4,5});
        System.out.println(buffer.hasRemaining());
        buffer.put(new byte[]{6,7,8,9,0});
        System.out.println(buffer.hasRemaining());

    }
}
