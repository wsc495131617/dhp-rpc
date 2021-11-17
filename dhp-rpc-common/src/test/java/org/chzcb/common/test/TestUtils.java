package org.chzcb.common.test;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.utils.AbstractIDGenerator;
import org.dhp.common.utils.LongUtils;
import org.dhp.common.utils.WorkIdSequenceGenerator;

@Slf4j
public class TestUtils {
    public static void main(String[] args) {
//        System.out.println(LocalIPUtils.hostName());
//        System.out.println(LocalIPUtils.resolveIp());
        WorkIdSequenceGenerator generator = new WorkIdSequenceGenerator((int) 'x');
        long id = generator.make();
        String seedStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890_";
        //64位的long转成 64进制的数字
        String newid = LongUtils.toShortString(id, 6);
        long revertId = LongUtils.parseLong(newid, 6);
        long st = System.nanoTime();
        for(int i=0;i<10000000;i++) {
            newid = LongUtils.toShortString(id, 6);
            revertId = LongUtils.parseLong(newid, 6);
        }
        newid = LongUtils.toShortString(id, 1);
        revertId = LongUtils.parseLong(newid, 1);
        System.out.println(System.nanoTime()-st);
        System.out.println(id + "-" + newid + "-" + revertId);
    }


}
