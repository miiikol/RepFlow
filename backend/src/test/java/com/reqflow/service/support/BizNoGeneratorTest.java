package com.reqflow.service.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class BizNoGeneratorTest {

    @Autowired
    private BizNoGenerator generator;

    @Test
    void concurrentCallsProduceUniqueNumbers() throws Exception {
        int total = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < total; i++) {
                futures.add(pool.submit(() -> generator.next("TST")));
            }
            Set<String> numbers = new HashSet<>();
            for (Future<String> future : futures) {
                numbers.add(future.get());
            }
            assertEquals(total, numbers.size(), "并发取号不允许重复");
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            assertTrue(numbers.stream().allMatch(n -> n.startsWith("TST-" + month + "-")));
        } finally {
            pool.shutdownNow();
        }
    }
}
