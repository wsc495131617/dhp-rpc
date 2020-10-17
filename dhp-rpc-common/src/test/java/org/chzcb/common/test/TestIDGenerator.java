package org.chzcb.common.test;

import org.dhp.common.utils.WorkIdSequenceGenerator;

public class TestIDGenerator {
    public static void main(String[] args) throws Throwable{
        WorkIdSequenceGenerator generator = new WorkIdSequenceGenerator(23);
        for(int i=0;i<1000;i++){
            long id = generator.make();
            System.out.println(generator.formatId(id));
            Thread.sleep(10);
        }
    }
}
