package com.hm.v1.sample;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Test {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            long id = IdWorker.id();
            log.info("默认十进制展示：{}",String.valueOf(id));
            log.info("转化为二进制展示：{}",String.valueOf(Long.toBinaryString(id)));
        }
    }
}
