package com.hyperfetch.repo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 任务分块数据访问对象
 * 提供任务分块的增删改查操作
 */
@Dao
public interface TaskChunkDao {
    
    /**
     * 根据任务ID获取所有分块
     * @param taskId 任务ID
     * @return 分块列表
     */
    @Query("SELECT * FROM task_chunks WHERE taskId = :taskId")
    List<TaskChunkEntity> getByTaskId(String taskId);
    
    /**
     * 批量插入分块
     * @param chunks 分块列表
     */
    @Insert
    void insertAll(List<TaskChunkEntity> chunks);
    
    /**
     * 更新分块
     * @param chunk 分块实体
     */
    @Update
    void update(TaskChunkEntity chunk);
    
    /**
     * 根据任务ID删除所有分块
     * @param taskId 任务ID
     */
    @Query("DELETE FROM task_chunks WHERE taskId = :taskId")
    void deleteByTaskId(String taskId);
}