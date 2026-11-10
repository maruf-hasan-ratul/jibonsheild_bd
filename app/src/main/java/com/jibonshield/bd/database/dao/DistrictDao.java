package com.jibonshield.bd.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jibonshield.bd.database.entity.DistrictEntity;

import java.util.List;

@Dao
public interface DistrictDao {

    @Query("SELECT * FROM districts WHERE isActive = 1 ORDER BY name ASC")
    List<DistrictEntity> getAllActive();

    @Query("SELECT * FROM districts ORDER BY name ASC")
    List<DistrictEntity> getAll();

    @Query("SELECT * FROM districts WHERE divisionId = :divisionId AND isActive = 1 ORDER BY name ASC")
    List<DistrictEntity> getByDivisionId(String divisionId);

    @Query("SELECT * FROM districts WHERE id = :id LIMIT 1")
    DistrictEntity getById(String id);

    @Query("SELECT * FROM districts WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    DistrictEntity getByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DistrictEntity> districts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DistrictEntity district);

    @Query("SELECT COUNT(*) FROM districts")
    int getCount();

    @Query("DELETE FROM districts")
    void deleteAll();
}

