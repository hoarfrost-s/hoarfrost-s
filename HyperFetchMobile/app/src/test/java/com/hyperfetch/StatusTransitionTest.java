package com.hyperfetch;

import com.hyperfetch.model.TaskStatus;
import com.hyperfetch.service.StatusTransition;
import com.hyperfetch.service.StatusTransition.TransitionReason;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * 状态流转单元测试
 * 测试所有状态转换规则、非法转换拒绝
 */
public class StatusTransitionTest {

    @Before
    public void setUp() {
        // 状态流转规则是静态定义的，无需初始化
    }

    // ==================== 状态转换规则测试 ====================

    /**
     * 测试 QUEUED -> DOWNLOADING 转换
     * 验证：QUEUED 状态可以转换到 DOWNLOADING 状态
     */
    @Test
    public void testQueuedToDownloadingTransition() {
        // 验证转换合法
        assertTrue("QUEUED -> DOWNLOADING 应该合法",
                StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.DOWNLOADING));
        
        // 验证允许的原因
        assertTrue("SCHEDULE 原因应该允许",
                StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.DOWNLOADING, TransitionReason.SCHEDULE));
        
        // 验证不允许的原因
        assertFalse("USER_PAUSE 原因不应该允许",
                StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.DOWNLOADING, TransitionReason.USER_PAUSE));
    }

    /**
     * 测试 DOWNLOADING -> PAUSED 转换
     * 验证：DOWNLOADING 状态可以转换到 PAUSED 状态
     */
    @Test
    public void testDownloadingToPausedTransition() {
        // 验证转换合法
        assertTrue("DOWNLOADING -> PAUSED 应该合法",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.PAUSED));
        
        // 验证允许的原因
        assertTrue("USER_PAUSE 原因应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.PAUSED, TransitionReason.USER_PAUSE));
        assertTrue("WIFI_DISCONNECTED 原因应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.PAUSED, TransitionReason.WIFI_DISCONNECTED));
        
        // 验证不允许的原因
        assertFalse("SCHEDULE 原因不应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.PAUSED, TransitionReason.SCHEDULE));
    }

    /**
     * 测试 DOWNLOADING -> COMPLETED 转换
     * 验证：DOWNLOADING 状态可以转换到 COMPLETED 状态
     */
    @Test
    public void testDownloadingToCompletedTransition() {
        // 验证转换合法
        assertTrue("DOWNLOADING -> COMPLETED 应该合法",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED));
        
        // 验证允许的原因
        assertTrue("DOWNLOAD_COMPLETE 原因应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED, TransitionReason.DOWNLOAD_COMPLETE));
        
        // 验证不允许的原因
        assertFalse("USER_PAUSE 原因不应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED, TransitionReason.USER_PAUSE));
    }

    /**
     * 测试 DOWNLOADING -> FAILED 转换
     * 验证：DOWNLOADING 状态可以转换到 FAILED 状态
     */
    @Test
    public void testDownloadingToFailedTransition() {
        // 验证转换合法
        assertTrue("DOWNLOADING -> FAILED 应该合法",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.FAILED));
        
        // 验证允许的原因
        assertTrue("DOWNLOAD_ERROR 原因应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.FAILED, TransitionReason.DOWNLOAD_ERROR));
        assertTrue("RETRIES_EXHAUSTED 原因应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.FAILED, TransitionReason.RETRIES_EXHAUSTED));
        
        // 验证不允许的原因
        assertFalse("USER_PAUSE 原因不应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.FAILED, TransitionReason.USER_PAUSE));
    }

    /**
     * 测试 PAUSED -> DOWNLOADING 转换
     * 验证：PAUSED 状态可以转换到 DOWNLOADING 状态
     */
    @Test
    public void testPausedToDownloadingTransition() {
        // 验证转换合法
        assertTrue("PAUSED -> DOWNLOADING 应该合法",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.DOWNLOADING));
        
        // 验证允许的原因
        assertTrue("USER_RESUME 原因应该允许",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.DOWNLOADING, TransitionReason.USER_RESUME));
        assertTrue("WIFI_RECONNECTED 原因应该允许",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.DOWNLOADING, TransitionReason.WIFI_RECONNECTED));
        
        // 验证不允许的原因
        assertFalse("SCHEDULE 原因不应该允许",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.DOWNLOADING, TransitionReason.SCHEDULE));
    }

    /**
     * 测试 FAILED -> DOWNLOADING 转换
     * 验证：FAILED 状态可以转换到 DOWNLOADING 状态
     */
    @Test
    public void testFailedToDownloadingTransition() {
        // 验证转换合法
        assertTrue("FAILED -> DOWNLOADING 应该合法",
                StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.DOWNLOADING));
        
        // 验证允许的原因
        assertTrue("USER_RETRY 原因应该允许",
                StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.DOWNLOADING, TransitionReason.USER_RETRY));
        
        // 验证不允许的原因
        assertFalse("SCHEDULE 原因不应该允许",
                StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.DOWNLOADING, TransitionReason.SCHEDULE));
    }

    // ==================== 终态测试 ====================

    /**
     * 测试 COMPLETED 终态
     * 验证：COMPLETED 状态不允许转换到任何其他状态
     */
    @Test
    public void testCompletedTerminalState() {
        // 验证 COMPLETED 是终态
        assertTrue("COMPLETED 应该是终态",
                StatusTransition.isTerminalStatus(TaskStatus.COMPLETED));
        
        // 验证不允许转换到任何状态
        assertFalse("COMPLETED -> DOWNLOADING 不应该允许",
                StatusTransition.canTransition(TaskStatus.COMPLETED, TaskStatus.DOWNLOADING));
        assertFalse("COMPLETED -> PAUSED 不应该允许",
                StatusTransition.canTransition(TaskStatus.COMPLETED, TaskStatus.PAUSED));
        assertFalse("COMPLETED -> FAILED 不应该允许",
                StatusTransition.canTransition(TaskStatus.COMPLETED, TaskStatus.FAILED));
        assertFalse("COMPLETED -> QUEUED 不应该允许",
                StatusTransition.canTransition(TaskStatus.COMPLETED, TaskStatus.QUEUED));
    }

    /**
     * 测试 COMPLETED_WITH_WARNING 终态
     * 验证：COMPLETED_WITH_WARNING 状态不允许转换到任何其他状态
     */
    @Test
    public void testCompletedWithWarningTerminalState() {
        // 验证 COMPLETED_WITH_WARNING 是终态
        assertTrue("COMPLETED_WITH_WARNING 应该是终态",
                StatusTransition.isTerminalStatus(TaskStatus.COMPLETED_WITH_WARNING));
        
        // 验证不允许转换到任何状态
        assertFalse("COMPLETED_WITH_WARNING -> DOWNLOADING 不应该允许",
                StatusTransition.canTransition(TaskStatus.COMPLETED_WITH_WARNING, TaskStatus.DOWNLOADING));
        assertFalse("COMPLETED_WITH_WARNING -> PAUSED 不应该允许",
                StatusTransition.canTransition(TaskStatus.COMPLETED_WITH_WARNING, TaskStatus.PAUSED));
        assertFalse("COMPLETED_WITH_WARNING -> FAILED 不应该允许",
                StatusTransition.canTransition(TaskStatus.COMPLETED_WITH_WARNING, TaskStatus.FAILED));
    }

    // ==================== 非法转换拒绝测试 ====================

    /**
     * 测试 QUEUED 非法转换
     * 验证：QUEUED 状态只能转换到 DOWNLOADING
     */
    @Test
    public void testQueuedIllegalTransitions() {
        // QUEUED 不能直接转换到 PAUSED
        assertFalse("QUEUED -> PAUSED 不应该允许",
                StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.PAUSED));
        
        // QUEUED 不能直接转换到 COMPLETED
        assertFalse("QUEUED -> COMPLETED 不应该允许",
                StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.COMPLETED));
        
        // QUEUED 不能直接转换到 FAILED
        assertFalse("QUEUED -> FAILED 不应该允许",
                StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.FAILED));
        
        // QUEUED 不能转换到自身
        assertFalse("QUEUED -> QUEUED 不应该允许",
                StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.QUEUED));
    }

    /**
     * 测试 DOWNLOADING 非法转换
     * 验证：DOWNLOADING 状态只能转换到 PAUSED、COMPLETED、FAILED
     */
    @Test
    public void testDownloadingIllegalTransitions() {
        // DOWNLOADING 不能转换到 QUEUED
        assertFalse("DOWNLOADING -> QUEUED 不应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.QUEUED));
        
        // DOWNLOADING 不能转换到自身
        assertFalse("DOWNLOADING -> DOWNLOADING 不应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.DOWNLOADING));
        
        // DOWNLOADING 不能转换到 COMPLETED_WITH_WARNING（通过其他路径）
        // 注意：COMPLETED_WITH_WARNING 是终态，不能从 DOWNLOADING 直接转换
    }

    /**
     * 测试 PAUSED 非法转换
     * 验证：PAUSED 状态只能转换到 DOWNLOADING
     */
    @Test
    public void testPausedIllegalTransitions() {
        // PAUSED 不能转换到 QUEUED
        assertFalse("PAUSED -> QUEUED 不应该允许",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.QUEUED));
        
        // PAUSED 不能直接转换到 COMPLETED
        assertFalse("PAUSED -> COMPLETED 不应该允许",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.COMPLETED));
        
        // PAUSED 不能直接转换到 FAILED
        assertFalse("PAUSED -> FAILED 不应该允许",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.FAILED));
        
        // PAUSED 不能转换到自身
        assertFalse("PAUSED -> PAUSED 不应该允许",
                StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.PAUSED));
    }

    /**
     * 测试 FAILED 非法转换
     * 验证：FAILED 状态只能转换到 DOWNLOADING
     */
    @Test
    public void testFailedIllegalTransitions() {
        // FAILED 不能转换到 QUEUED
        assertFalse("FAILED -> QUEUED 不应该允许",
                StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.QUEUED));
        
        // FAILED 不能直接转换到 PAUSED
        assertFalse("FAILED -> PAUSED 不应该允许",
                StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.PAUSED));
        
        // FAILED 不能直接转换到 COMPLETED
        assertFalse("FAILED -> COMPLETED 不应该允许",
                StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.COMPLETED));
        
        // FAILED 不能转换到自身
        assertFalse("FAILED -> FAILED 不应该允许",
                StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.FAILED));
    }

    /**
     * 测试 null 状态转换
     * 验证：null 状态不允许任何转换
     */
    @Test
    public void testNullStateTransition() {
        // null -> 任何状态都不允许
        assertFalse("null -> DOWNLOADING 不应该允许",
                StatusTransition.canTransition(null, TaskStatus.DOWNLOADING));
        assertFalse("DOWNLOADING -> null 不应该允许",
                StatusTransition.canTransition(TaskStatus.DOWNLOADING, null));
        assertFalse("null -> null 不应该允许",
                StatusTransition.canTransition(null, null));
    }

    // ==================== 获取允许目标状态测试 ====================

    /**
     * 测试获取 QUEUED 允许的目标状态
     * 验证：QUEUED 只允许转换到 DOWNLOADING
     */
    @Test
    public void testGetAllowedTargetsForQueued() {
        Set<TaskStatus> targets = StatusTransition.getAllowedTargets(TaskStatus.QUEUED);
        
        // 验证只有一个目标状态
        assertEquals(1, targets.size());
        assertTrue(targets.contains(TaskStatus.DOWNLOADING));
    }

    /**
     * 测试获取 DOWNLOADING 允许的目标状态
     * 验证：DOWNLOADING 允许转换到 PAUSED、COMPLETED、FAILED
     */
    @Test
    public void testGetAllowedTargetsForDownloading() {
        Set<TaskStatus> targets = StatusTransition.getAllowedTargets(TaskStatus.DOWNLOADING);
        
        // 验证有三个目标状态
        assertEquals(3, targets.size());
        assertTrue(targets.contains(TaskStatus.PAUSED));
        assertTrue(targets.contains(TaskStatus.COMPLETED));
        assertTrue(targets.contains(TaskStatus.FAILED));
    }

    /**
     * 测试获取 PAUSED 允许的目标状态
     * 验证：PAUSED 只允许转换到 DOWNLOADING
     */
    @Test
    public void testGetAllowedTargetsForPaused() {
        Set<TaskStatus> targets = StatusTransition.getAllowedTargets(TaskStatus.PAUSED);
        
        // 验证只有一个目标状态
        assertEquals(1, targets.size());
        assertTrue(targets.contains(TaskStatus.DOWNLOADING));
    }

    /**
     * 测试获取 FAILED 允许的目标状态
     * 验证：FAILED 只允许转换到 DOWNLOADING
     */
    @Test
    public void testGetAllowedTargetsForFailed() {
        Set<TaskStatus> targets = StatusTransition.getAllowedTargets(TaskStatus.FAILED);
        
        // 验证只有一个目标状态
        assertEquals(1, targets.size());
        assertTrue(targets.contains(TaskStatus.DOWNLOADING));
    }

    /**
     * 测试获取 COMPLETED 允许的目标状态
     * 验证：COMPLETED 不允许任何转换
     */
    @Test
    public void testGetAllowedTargetsForCompleted() {
        Set<TaskStatus> targets = StatusTransition.getAllowedTargets(TaskStatus.COMPLETED);
        
        // 验证没有目标状态
        assertEquals(0, targets.size());
    }

    // ==================== 获取允许原因测试 ====================

    /**
     * 测试获取 QUEUED -> DOWNLOADING 允许的原因
     * 验证：只允许 SCHEDULE 原因
     */
    @Test
    public void testGetAllowedReasonsForQueuedToDownloading() {
        Set<TransitionReason> reasons = StatusTransition.getAllowedReasons(TaskStatus.QUEUED, TaskStatus.DOWNLOADING);
        
        // 验证只有一个原因
        assertEquals(1, reasons.size());
        assertTrue(reasons.contains(TransitionReason.SCHEDULE));
    }

    /**
     * 测试获取 DOWNLOADING -> PAUSED 允许的原因
     * 验证：允许 USER_PAUSE 和 WIFI_DISCONNECTED
     */
    @Test
    public void testGetAllowedReasonsForDownloadingToPaused() {
        Set<TransitionReason> reasons = StatusTransition.getAllowedReasons(TaskStatus.DOWNLOADING, TaskStatus.PAUSED);
        
        // 验证有两个原因
        assertEquals(2, reasons.size());
        assertTrue(reasons.contains(TransitionReason.USER_PAUSE));
        assertTrue(reasons.contains(TransitionReason.WIFI_DISCONNECTED));
    }

    /**
     * 测试获取 DOWNLOADING -> COMPLETED 允许的原因
     * 验证：只允许 DOWNLOAD_COMPLETE
     */
    @Test
    public void testGetAllowedReasonsForDownloadingToCompleted() {
        Set<TransitionReason> reasons = StatusTransition.getAllowedReasons(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED);
        
        // 验证只有一个原因
        assertEquals(1, reasons.size());
        assertTrue(reasons.contains(TransitionReason.DOWNLOAD_COMPLETE));
    }

    /**
     * 测试获取 DOWNLOADING -> FAILED 允许的原因
     * 验证：允许 DOWNLOAD_ERROR 和 RETRIES_EXHAUSTED
     */
    @Test
    public void testGetAllowedReasonsForDownloadingToFailed() {
        Set<TransitionReason> reasons = StatusTransition.getAllowedReasons(TaskStatus.DOWNLOADING, TaskStatus.FAILED);
        
        // 验证有两个原因
        assertEquals(2, reasons.size());
        assertTrue(reasons.contains(TransitionReason.DOWNLOAD_ERROR));
        assertTrue(reasons.contains(TransitionReason.RETRIES_EXHAUSTED));
    }

    /**
     * 测试获取 PAUSED -> DOWNLOADING 允许的原因
     * 验证：允许 USER_RESUME 和 WIFI_RECONNECTED
     */
    @Test
    public void testGetAllowedReasonsForPausedToDownloading() {
        Set<TransitionReason> reasons = StatusTransition.getAllowedReasons(TaskStatus.PAUSED, TaskStatus.DOWNLOADING);
        
        // 验证有两个原因
        assertEquals(2, reasons.size());
        assertTrue(reasons.contains(TransitionReason.USER_RESUME));
        assertTrue(reasons.contains(TransitionReason.WIFI_RECONNECTED));
    }

    /**
     * 测试获取 FAILED -> DOWNLOADING 允许的原因
     * 验证：只允许 USER_RETRY
     */
    @Test
    public void testGetAllowedReasonsForFailedToDownloading() {
        Set<TransitionReason> reasons = StatusTransition.getAllowedReasons(TaskStatus.FAILED, TaskStatus.DOWNLOADING);
        
        // 验证只有一个原因
        assertEquals(1, reasons.size());
        assertTrue(reasons.contains(TransitionReason.USER_RETRY));
    }

    /**
     * 测试获取非法转换的原因
     * 验证：非法转换返回空集合
     */
    @Test
    public void testGetAllowedReasonsForIllegalTransition() {
        Set<TransitionReason> reasons = StatusTransition.getAllowedReasons(TaskStatus.QUEUED, TaskStatus.PAUSED);
        
        // 验证返回空集合
        assertEquals(0, reasons.size());
    }

    // ==================== 辅助方法测试 ====================

    /**
     * 测试判断活跃状态
     * 验证：只有 DOWNLOADING 是活跃状态
     */
    @Test
    public void testIsActiveStatus() {
        // DOWNLOADING 是活跃状态
        assertTrue(StatusTransition.isActiveStatus(TaskStatus.DOWNLOADING));
        
        // 其他状态不是活跃状态
        assertFalse(StatusTransition.isActiveStatus(TaskStatus.QUEUED));
        assertFalse(StatusTransition.isActiveStatus(TaskStatus.PAUSED));
        assertFalse(StatusTransition.isActiveStatus(TaskStatus.COMPLETED));
        assertFalse(StatusTransition.isActiveStatus(TaskStatus.FAILED));
        assertFalse(StatusTransition.isActiveStatus(TaskStatus.COMPLETED_WITH_WARNING));
    }

    /**
     * 测试判断可恢复状态
     * 验证：PAUSED 和 FAILED 是可恢复状态
     */
    @Test
    public void testIsResumableStatus() {
        // PAUSED 和 FAILED 是可恢复状态
        assertTrue(StatusTransition.isResumableStatus(TaskStatus.PAUSED));
        assertTrue(StatusTransition.isResumableStatus(TaskStatus.FAILED));
        
        // 其他状态不是可恢复状态
        assertFalse(StatusTransition.isResumableStatus(TaskStatus.QUEUED));
        assertFalse(StatusTransition.isResumableStatus(TaskStatus.DOWNLOADING));
        assertFalse(StatusTransition.isResumableStatus(TaskStatus.COMPLETED));
        assertFalse(StatusTransition.isResumableStatus(TaskStatus.COMPLETED_WITH_WARNING));
    }

    /**
     * 测试判断终态
     * 验证：COMPLETED 和 COMPLETED_WITH_WARNING 是终态
     */
    @Test
    public void testIsTerminalStatus() {
        // COMPLETED 和 COMPLETED_WITH_WARNING 是终态
        assertTrue(StatusTransition.isTerminalStatus(TaskStatus.COMPLETED));
        assertTrue(StatusTransition.isTerminalStatus(TaskStatus.COMPLETED_WITH_WARNING));
        
        // 其他状态不是终态
        assertFalse(StatusTransition.isTerminalStatus(TaskStatus.QUEUED));
        assertFalse(StatusTransition.isTerminalStatus(TaskStatus.DOWNLOADING));
        assertFalse(StatusTransition.isTerminalStatus(TaskStatus.PAUSED));
        assertFalse(StatusTransition.isTerminalStatus(TaskStatus.FAILED));
    }

    // ==================== 状态转换原因枚举测试 ====================

    /**
     * 测试转换原因枚举完整性
     * 验证：所有转换原因都存在
     */
    @Test
    public void testTransitionReasonEnum() {
        TransitionReason[] reasons = TransitionReason.values();
        
        // 验证有 8 个转换原因
        assertEquals(8, reasons.length);
        
        // 验证原因名称
        assertEquals("SCHEDULE", TransitionReason.SCHEDULE.name());
        assertEquals("USER_PAUSE", TransitionReason.USER_PAUSE.name());
        assertEquals("WIFI_DISCONNECTED", TransitionReason.WIFI_DISCONNECTED.name());
        assertEquals("USER_RESUME", TransitionReason.USER_RESUME.name());
        assertEquals("WIFI_RECONNECTED", TransitionReason.WIFI_RECONNECTED.name());
        assertEquals("DOWNLOAD_COMPLETE", TransitionReason.DOWNLOAD_COMPLETE.name());
        assertEquals("DOWNLOAD_ERROR", TransitionReason.DOWNLOAD_ERROR.name());
        assertEquals("RETRIES_EXHAUSTED", TransitionReason.RETRIES_EXHAUSTED.name());
        assertEquals("USER_RETRY", TransitionReason.USER_RETRY.name());
    }

    // ==================== 状态枚举测试 ====================

    /**
     * 测试状态枚举完整性
     * 验证：所有状态都存在
     */
    @Test
    public void testTaskStatusEnum() {
        TaskStatus[] statuses = TaskStatus.values();
        
        // 验证有 6 个状态
        assertEquals(6, statuses.length);
        
        // 验证状态名称
        assertEquals("QUEUED", TaskStatus.QUEUED.name());
        assertEquals("DOWNLOADING", TaskStatus.DOWNLOADING.name());
        assertEquals("PAUSED", TaskStatus.PAUSED.name());
        assertEquals("COMPLETED", TaskStatus.COMPLETED.name());
        assertEquals("FAILED", TaskStatus.FAILED.name());
        assertEquals("COMPLETED_WITH_WARNING", TaskStatus.COMPLETED_WITH_WARNING.name());
    }

    // ==================== 综合状态流转测试 ====================

    /**
     * 测试完整的状态流转路径
     * 验证：从 QUEUED 到 COMPLETED 的完整流转路径
     */
    @Test
    public void testCompleteTransitionPath() {
        // QUEUED -> DOWNLOADING（调度开始）
        assertTrue(StatusTransition.canTransition(TaskStatus.QUEUED, TaskStatus.DOWNLOADING, TransitionReason.SCHEDULE));
        
        // DOWNLOADING -> COMPLETED（下载完成）
        assertTrue(StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED, TransitionReason.DOWNLOAD_COMPLETE));
        
        // COMPLETED 是终态，不能继续转换
        assertTrue(StatusTransition.isTerminalStatus(TaskStatus.COMPLETED));
    }

    /**
     * 测试暂停恢复流转路径
     * 验证：从 DOWNLOADING 到 PAUSED 再恢复的流转路径
     */
    @Test
    public void testPauseResumeTransitionPath() {
        // DOWNLOADING -> PAUSED（用户暂停）
        assertTrue(StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.PAUSED, TransitionReason.USER_PAUSE));
        
        // PAUSED -> DOWNLOADING（用户恢复）
        assertTrue(StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.DOWNLOADING, TransitionReason.USER_RESUME));
        
        // DOWNLOADING -> COMPLETED（下载完成）
        assertTrue(StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED, TransitionReason.DOWNLOAD_COMPLETE));
    }

    /**
     * 测试失败重试流转路径
     * 验证：从 DOWNLOADING 到 FAILED 再重试的流转路径
     */
    @Test
    public void testFailRetryTransitionPath() {
        // DOWNLOADING -> FAILED（下载错误）
        assertTrue(StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.FAILED, TransitionReason.DOWNLOAD_ERROR));
        
        // FAILED -> DOWNLOADING（用户重试）
        assertTrue(StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.DOWNLOADING, TransitionReason.USER_RETRY));
        
        // DOWNLOADING -> COMPLETED（下载完成）
        assertTrue(StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED, TransitionReason.DOWNLOAD_COMPLETE));
    }

    /**
     * 测试 Wi-Fi 断开恢复流转路径
     * 验证：Wi-Fi 断开导致的暂停和恢复流转路径
     */
    @Test
    public void testWifiDisconnectReconnectPath() {
        // DOWNLOADING -> PAUSED（Wi-Fi 断开）
        assertTrue(StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.PAUSED, TransitionReason.WIFI_DISCONNECTED));
        
        // PAUSED -> DOWNLOADING（Wi-Fi 恢复）
        assertTrue(StatusTransition.canTransition(TaskStatus.PAUSED, TaskStatus.DOWNLOADING, TransitionReason.WIFI_RECONNECTED));
    }

    /**
     * 测试重试次数用尽流转路径
     * 验证：重试次数用尽导致失败
     */
    @Test
    public void testRetriesExhaustedPath() {
        // DOWNLOADING -> FAILED（重试次数用尽）
        assertTrue(StatusTransition.canTransition(TaskStatus.DOWNLOADING, TaskStatus.FAILED, TransitionReason.RETRIES_EXHAUSTED));
        
        // FAILED 可以重试
        assertTrue(StatusTransition.canTransition(TaskStatus.FAILED, TaskStatus.DOWNLOADING, TransitionReason.USER_RETRY));
    }
}