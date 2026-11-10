package com.jibonshield.bd.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jibonshield.bd.database.entity.SyncMetadataEntity;

import java.util.List;

@Dao
public interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE tableName = :tableName LIMIT 1")
    SyncMetadataEntity getByTable(String tableName);

    @Query("SELECT * FROM sync_metadata")
    List<SyncMetadataEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SyncMetadataEntity metadata);

    @Query("DELETE FROM sync_metadata WHERE tableName = :tableName")
    void deleteByTable(String tableName);

    @Query("DELETE FROM sync_metadata")
    void deleteAll();
}

