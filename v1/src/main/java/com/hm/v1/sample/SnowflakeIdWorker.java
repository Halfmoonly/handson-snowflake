package com.hm.v1.sample;

/**
 * Twitter的分布式自增ID算法snowflake实现。
 *
 * 异或( ^ )  两个数相同，结果为0，不相同则为1
 * 左移( << )  二进制向左移多少位，低位补0
 * 与( & ) 位运算两个数都为1，结果为1，否则为0
 * 与( | ) 两个数只要一个是1，结果为1，否则为0
 */
public  class SnowflakeIdWorker {
    // 时间戳起始偏移，2022-04-01
    private final long startTime = 1648742400000L;

    // 序列号占用的位数
    private final long sequenceBits = 12L;
    // 数据中心ID占用的位数
    private final long datacenterIdBits = 5L;
    // 工作机器ID占用的位数
    private final long workerIdBits = 5L;

    // 支持的最大工作机器ID，结果是31 (即最大值为：2^5 - 1)
    private final long maxWorkerId = ~(-1L << workerIdBits);
    // 支持的最大数据中心ID，结果是31 (即最大值为：2^5 - 1)
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    // 序列在id中占的位数，结果是4095 (即最大值为：2^12 - 1)
    private final long sequenceMask = ~(-1L << sequenceBits);

    // 时间戳左移位数
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    // 数据标识ID左移位数
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    // 工作机器ID左移位数
    private final long workerIdShift = sequenceBits;

    // 工作机器ID
    private final long workerId;
    // 数据中心ID
    private final long datacenterId;
    // 毫秒内序列
    private long sequence = 0L;
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;

    /**
     * 构造函数。
     *
     * @param workerId      工作机器ID。
     * @param datacenterId 数据中心ID。
     */
    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 获取下一个唯一ID。
     *
     * @return 下一个唯一ID。
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上次时间戳，则时钟回拨，抛出异常
        // todo
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 如果同一毫秒内，则进行序列号加一, 因为未来snowflake服务会做高可用，synchronized锁不住
        if (lastTimestamp == timestamp) {
            //sequenceMask是低16为全1， 当(sequence + 1)==4096即低16位全0
            sequence = (sequence + 1) & sequenceMask;
            // 同一毫秒内的序列溢出怎么办？
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp); // 阻塞到下一毫秒
            }
        } else {
            // 不同毫秒内，序列号置零
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // ID组合与移位
        return ((timestamp - startTime) << timestampLeftShift) |//左移12+10=22位
                (datacenterId << datacenterIdShift) |//左移12+5=17位
                (workerId << workerIdShift) |//左移12位
                sequence;
    }

    /**
     * 阻塞直到获得一个新的时间戳。
     *
     * @param lastTimestamp 最后一次时间戳。
     * @return 当前时间戳。
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（以毫秒为单位）。
     *
     * @return 当前时间戳。
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
