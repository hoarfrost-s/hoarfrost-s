package com.hyperfetch;

import com.hyperfetch.rate.TokenBucketLimiter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 令牌桶限速器单元测试
 * 测试最小速率约束、令牌申领和阻塞、速率调整、无限制模式
 */
public class TokenBucketLimiterTest {

    /** 最小速率：16 KB/s = 16384 字节/秒 */
    private static final long MIN_RATE = 16 * 1024;

    private TokenBucketLimiter limiter;

    @Before
    public void setUp() {
        // 每个测试前创建新的限速器实例
        limiter = new TokenBucketLimiter();
    }

    /**
     * 测试最小速率约束（16 KB/s）
     * 验证：设置低于最小速率时，自动调整为最小速率
     */
    @Test
    public void testMinRateConstraint() {
        // 创建限速器并设置低于最小速率
        TokenBucketLimiter limiterWithLowRate = new TokenBucketLimiter(1000); // 1 KB/s
        
        // 验证速率被自动调整为最小速率 16 KB/s
        assertEquals(MIN_RATE, limiterWithLowRate.getRate());

        // 使用 setRate 设置低于最小速率
        limiter.setRate(5000); // 5 KB/s
        
        // 验证速率被自动调整为最小速率
        assertEquals(MIN_RATE, limiter.getRate());
    }

    /**
     * 测试设置正常速率（高于最小速率）
     * 验证：设置高于最小速率时，速率正常设置
     */
    @Test
    public void testNormalRateSetting() {
        long normalRate = 100 * 1024; // 100 KB/s
        
        limiter.setRate(normalRate);
        
        // 验证速率正常设置
        assertEquals(normalRate, limiter.getRate());
    }

    /**
     * 测试令牌申领 - 无阻塞场景
     * 验证：当桶内有足够令牌时，申领立即成功
     */
    @Test
    public void testAcquireWithoutBlocking() throws InterruptedException {
        // 设置速率并初始化令牌桶
        long rate = 100 * 1024; // 100 KB/s
        limiter.setRate(rate);
        
        // 等待一小段时间让令牌填充
        Thread.sleep(100);
        
        // 申领小量令牌（应该立即成功）
        int smallBytes = 1024; // 1 KB
        long startTime = System.currentTimeMillis();
        limiter.acquire(smallBytes);
        long endTime = System.currentTimeMillis();
        
        // 验证申领几乎立即完成（不超过 100ms）
        assertTrue("申领应该立即完成", endTime - startTime < 100);
    }

    /**
     * 测试令牌申领和阻塞
     * 验证：当桶内令牌不足时，申领会阻塞等待
     */
    @Test
    public void testAcquireWithBlocking() throws InterruptedException {
        // 设置较低速率便于测试阻塞
        long rate = MIN_RATE; // 16 KB/s
        TokenBucketLimiter slowLimiter = new TokenBucketLimiter(rate);
        
        // 先申领大量令牌消耗桶内令牌
        slowLimiter.acquire(16 * 1024); // 消耗所有令牌
        
        // 再次申领，应该会阻塞等待
        int bytesToAcquire = 8 * 1024; // 8 KB
        long startTime = System.currentTimeMillis();
        slowLimiter.acquire(bytesToAcquire);
        long endTime = System.currentTimeMillis();
        
        // 计算预期等待时间：8 KB / 16 KB/s = 0.5 秒 = 500ms
        long expectedWaitMs = bytesToAcquire * 1000 / rate;
        long actualWaitMs = endTime - startTime;
        
        // 验证阻塞时间接近预期（允许一定误差）
        assertTrue("应该有阻塞等待", actualWaitMs >= expectedWaitMs * 0.8);
    }

    /**
     * 测试速率动态调整
     * 验证：可以动态调整速率，调整后立即生效
     */
    @Test
    public void testRateAdjustment() throws InterruptedException {
        // 初始设置速率
        long initialRate = 50 * 1024; // 50 KB/s
        limiter.setRate(initialRate);
        assertEquals(initialRate, limiter.getRate());
        
        // 动态调整速率
        long newRate = 200 * 1024; // 200 KB/s
        limiter.setRate(newRate);
        assertEquals(newRate, limiter.getRate());
        
        // 再次调整到最小速率边界
        limiter.setRate(10 * 1024); // 10 KB/s（低于最小速率）
        assertEquals(MIN_RATE, limiter.getRate());
    }

    /**
     * 测试无限制模式
     * 验证：设置为 Long.MAX_VALUE 时，申领不阻塞
     */
    @Test
    public void testUnlimitedMode() throws InterruptedException {
        // 默认构造函数创建无限制模式
        TokenBucketLimiter unlimitedLimiter = new TokenBucketLimiter();
        
        // 验证初始速率是无限制
        assertEquals(Long.MAX_VALUE, unlimitedLimiter.getRate());
        
        // 申领大量令牌（应该立即成功）
        int largeBytes = 10 * 1024 * 1024; // 10 MB
        long startTime = System.currentTimeMillis();
        unlimitedLimiter.acquire(largeBytes);
        long endTime = System.currentTimeMillis();
        
        // 验证申领立即完成（不超过 50ms）
        assertTrue("无限制模式下申领应该立即完成", endTime - startTime < 50);
    }

    /**
     * 测试设置为无限制模式
     * 验证：可以动态切换到无限制模式
     */
    @Test
    public void testSwitchToUnlimitedMode() throws InterruptedException {
        // 先设置有限速率
        limiter.setRate(50 * 1024);
        assertEquals(50 * 1024, limiter.getRate());
        
        // 切换到无限制模式
        limiter.setRate(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, limiter.getRate());
        
        // 申领应该立即成功
        long startTime = System.currentTimeMillis();
        limiter.acquire(1024 * 1024);
        long endTime = System.currentTimeMillis();
        
        assertTrue("切换到无限制后申领应该立即完成", endTime - startTime < 50);
    }

    /**
     * 测试多次连续申领
     * 验证：连续申领会正确消耗令牌
     */
    @Test
    public void testMultipleAcquires() throws InterruptedException {
        // 设置速率
        long rate = 32 * 1024; // 32 KB/s
        TokenBucketLimiter testLimiter = new TokenBucketLimiter(rate);
        
        // 连续申领多次
        int acquireSize = 4 * 1024; // 4 KB
        for (int i = 0; i < 3; i++) {
            testLimiter.acquire(acquireSize);
        }
        
        // 验证速率保持不变
        assertEquals(rate, testLimiter.getRate());
    }

    /**
     * 测试令牌桶容量限制
     * 验证：令牌数不会超过桶容量（速率值）
     */
    @Test
    public void testBucketCapacity() throws InterruptedException {
        // 设置速率
        long rate = 16 * 1024; // 16 KB/s
        TokenBucketLimiter testLimiter = new TokenBucketLimiter(rate);
        
        // 等待足够时间让令牌填充到满
        Thread.sleep(2000); // 等待 2 秒
        
        // 申领令牌，验证不会超过桶容量
        testLimiter.acquire(16 * 1024);
        
        // 再次申领应该需要等待
        long startTime = System.currentTimeMillis();
        testLimiter.acquire(1024);
        long endTime = System.currentTimeMillis();
        
        // 应该有等待时间
        assertTrue("令牌不应超过桶容量", endTime - startTime > 50);
    }

    /**
     * 测试零速率处理
     * 验证：设置零速率会被调整为最小速率
     */
    @Test
    public void testZeroRateHandling() {
        limiter.setRate(0);
        
        // 验证零速率被调整为最小速率
        assertEquals(MIN_RATE, limiter.getRate());
    }

    /**
     * 测试负速率处理
     * 验证：设置负速率会被调整为最小速率
     */
    @Test
    public void testNegativeRateHandling() {
        limiter.setRate(-1000);
        
        // 验证负速率被调整为最小速率
        assertEquals(MIN_RATE, limiter.getRate());
    }
}