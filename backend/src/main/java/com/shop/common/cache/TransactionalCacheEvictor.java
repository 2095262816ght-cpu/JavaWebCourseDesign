package com.shop.common.cache;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 缓存淘汰执行器：先立即淘汰一次，若当前存在活动事务，则在事务提交后再次淘汰。
 *
 * <p>用于消除各 Service 中重复的 {@code TransactionSynchronization.afterCommit} 脚手架，
 * 防止并发读线程在事务提交前读到旧数据并回写 Redis 造成脏缓存。</p>
 */
public final class TransactionalCacheEvictor {

    private TransactionalCacheEvictor() {
    }

    /**
     * @param eviction 淘汰动作（应以 try/catch 自行吞掉 Redis 异常，避免缓存故障阻断主流程）
     */
    public static void evictNowAndAfterCommit(Runnable eviction) {
        eviction.run();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        }
    }
}
