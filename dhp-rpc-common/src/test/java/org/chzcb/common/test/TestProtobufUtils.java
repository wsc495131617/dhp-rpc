package org.chzcb.common.test;

import lombok.Builder;
import lombok.Data;
import org.dhp.common.utils.ProtostuffUtils;

import java.util.LinkedList;
import java.util.List;

public class TestProtobufUtils {
    @Data
    @Builder
    public static class TestObj1 {
        String name;
    }
    @Builder
    @Data
    public static class TestResp {
        List<TestObj1> list;
    }

    public static void main(String[] args) {
        TestResp resp = TestResp.builder().list(new LinkedList<>()).build();
        byte[] bytes = ProtostuffUtils.serialize(TestResp.class, resp);
        resp = ProtostuffUtils.deserialize(bytes, TestResp.class);
        System.out.println(resp);
    }
}
