package org.dhp.common.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 机构编号下的Id生成规则，能够确保机构下唯一，同时包含机构编号的信息存在
 * 当前获取的Long，位数超过53位，对于JavaScript来说会丢失精度，因此，使用的时候需要转化成String转给前端
 * <p>
 * [0] 符号保留位数
 * [1-31] 时间偏差秒数 0-2147483647 68年
 * [32-56] 2^25 sequenceId 每秒33554431个编号
 * [57-63] 0-127 workId 最大集群127
 * </p>
 * 通过定义来看，目前每个进程能够支持的是每秒2^25个并发，针对统一机构下，注册并发不高的情况才能使用<br/>
 * 支持机构编号最大为65535个，因此机构编号需要慎用<br />
 * 一般用于非高并发创建ID，但是需要包含机构编号的sequence规则
 */
@Slf4j
public class WorkIdSequenceGenerator extends AbstractIDGenerator {
    protected static int MaxID = 33554431;

    protected Integer workId;

    protected Long epoch;

    public WorkIdSequenceGenerator(Integer workId) {
        if (workId > 127) {
            throw new RuntimeException("work id must less then 127");
        }
        this.workId = workId;
        //2020-10-01 00:00:00 +8:00
        this.epoch = 1601481600l;
    }

    public String formatId(long id) {
        long[] arr = parseId(id);
        return String.format("ts:%d, sequeueId:%d, workId:%d", arr[0] * 1000, arr[1], arr[2], arr[3]);
    }

    private long[] parseId(long id) {
        long[] arr = new long[4];
        arr[3] = (id & this.diode(1L, 32L)) >> 32;
        arr[0] = arr[3] + 1601481600l;
        arr[1] = (id & this.diode(32L, 25L)) >> 7;
        arr[2] = id & this.diode(57L, 7L);
        return arr;
    }

    private long diode(long offset, long length) {
        int lb = (int) (64L - offset);
        int rb = (int) (64L - (offset + length));
        return -1L << lb ^ -1L << rb;
    }

    @Override
    public long make() {
        increment();
        //如果时间在epoch之前，那么id就是负数，这个时候需要核对系统时间
        long timeDiff = lastTs - epoch;
        long id = (timeDiff << 32) + (sequenceID << 7) + workId;
        return id;
    }
}
