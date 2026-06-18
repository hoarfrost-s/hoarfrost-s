package com.hyperfetch.repo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 下载任务实体类
 * 用于存储下载任务的基本信息
 */
@Entity(tableName = "download_tasks")
public class DownloadTaskEntity {
    
    /**
     * 任务唯一标识符
     */
    @PrimaryKey
    public String id;
    
    /**
     * 下载地址
     */
    public String url;
    
    /**
     * 文件名
     */
    public String fileName;
    
    /**
     * 文件总大小（字节）
     */
    public long totalSize;
    
    /**
     * 已下载大小（字节）
     */
    public long downloaded;
    
    /**
     * 任务分类
     */
    public String category;
    
    /**
     * 任务状态
     */
    public String status;
    
    /**
     * 任务优先级
     */
    public String priority;
    
    /**
     * 线程数
     */
    public int threadCount;
    
    /**
     * 速度限制（字节/秒）
     */
    public long speedLimit;
    
    /**
     * 保存路径
     */
    public String savePath;
    
    /**
     * 创建时间（时间戳）
     */
    public long createTime;
}