package com.hm.v1.sample;
import org.springframework.util.Assert;
public class IdWorker {
    /**
     * 默认的SnowflakeIdWorker实例，使用默认的workerId和datacenterId。
     */
    private static final SnowflakeIdWorker worker = new SnowflakeIdWorker(0, 0);

    /**
     * 获取下一个唯一ID。
     *
     * @return 下一个唯一ID。
     */
    public static long id() {
        Assert.notNull(worker, "SnowflakeIdWorker未配置!");
        return worker.nextId();
    }
}
