package com.hyperfetch.repo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 下载任务数据访问对象
 * 提供下载任务的增删改查操作
 */
@Dao
public interface DownloadTaskDao {
    
    /**
     * 获取所有下载任务，按创建时间降序排列
     * @return 任务列表的LiveData
     */
    @Query("SELECT * FROM download_tasks ORDER BY createTime DESC")
    LiveData<List<DownloadTaskEntity>> getAll();
    
    /**
     * 根据分类获取下载任务
     * @param category 分类名称
     * @return 任务列表的LiveData
     */
    @Query("SELECT * FROM download_tasks WHERE category = :category")
    LiveData<List<DownloadTaskEntity>> getByCategory(String category);
    
    /**
     * 根据ID获取下载任务
     * @param id 任务ID
     * @return 任务实体
     */
    @Query("SELECT * FROM download_tasks WHERE id = :id")
    DownloadTaskEntity getById(String id);
    
    /**
     * 插入下载任务
     * @param task 任务实体
     */
    @Insert
    void insert(DownloadTaskEntity task);
    
    /**
     * 更新下载任务
     * @param task 任务实体
     */
    @Update
    void update(DownloadTaskEntity task);
    
    /**
     * 删除下载任务
     * @param task 任务实体
     */
    @Delete
    void delete(DownloadTaskEntity task);

    /**
     * 获取所有未完成的下载任务（DOWNLOADING 和 PAUSED 状态）
     * 用于进程被杀后的自动恢复
     * @return 未完成任务列表
     */
    @Query("SELECT * FROM download_tasks WHERE status = 'DOWNLOADING' OR status = 'PAUSED'")
    List<DownloadTaskEntity> getUnfinishedTasks();

    /**
     * 根据状态获取下载任务列表
     * @param status 任务状态
     * @return 任务列表
     */
    @Query("SELECT * FROM download_tasks WHERE status = :status")
    List<DownloadTaskEntity> getByStatus(String status);
}