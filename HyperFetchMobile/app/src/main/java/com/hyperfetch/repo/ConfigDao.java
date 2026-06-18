package com.hyperfetch.repo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

/**
 * 配置数据访问对象
 * 提供调度器配置的增删改查操作
 */
@Dao
public interface ConfigDao {
    
    /**
     * 获取调度器配置
     * @return 配置实体
     */
    @Query("SELECT * FROM scheduler_config WHERE id = 1")
    ConfigEntity getConfig();
    
    /**
     * 插入配置
     * @param config 配置实体
     */
    @Insert
    void insert(ConfigEntity config);
    
    /**
     * 更新配置
     * @param config 配置实体
     */
    @Update
    void update(ConfigEntity config);
}