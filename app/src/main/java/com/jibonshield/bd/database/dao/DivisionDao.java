package com.jibonshield.bd.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jibonshield.bd.database.entity.DivisionEntity;

import java.util.List;

@Dao
public interface DivisionDao {

    @Query("SELECT * FROM divisions WHERE isActive = 1 ORDER BY name ASC")
    List<DivisionEntity> getAllActive();

    @Query("SELECT * FROM divisions ORDER BY name ASC")
    List<DivisionEntity> getAll();

    @Query("SELECT * FROM divisions WHERE id = :id LIMIT 1")
    DivisionEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DivisionEntity> divisions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DivisionEntity division);

    @Query("SELECT COUNT(*) FROM divisions")
    int getCount();

    @Query("DELETE FROM divisions")
    void deleteAll();
}

