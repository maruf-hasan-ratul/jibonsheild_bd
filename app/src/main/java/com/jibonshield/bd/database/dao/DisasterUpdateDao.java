package com.jibonshield.bd.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jibonshield.bd.database.entity.DisasterUpdateEntity;

import java.util.List;

@Dao
public interface DisasterUpdateDao {

    @Query("SELECT * FROM disaster_updates WHERE isActive = 1 ORDER BY issuedAt DESC")
    List<DisasterUpdateEntity> getAllActive();

    @Query("SELECT * FROM disaster_updates ORDER BY issuedAt DESC")
    List<DisasterUpdateEntity> getAll();

    @Query("SELECT * FROM disaster_updates WHERE id = :id LIMIT 1")
    DisasterUpdateEntity getById(String id);

    @Query("SELECT * FROM disaster_updates WHERE isActive = 1 AND (upazilaId = :upazilaId OR districtId = :districtId OR divisionId = :divisionId OR divisionId IS NULL OR divisionId = '' OR divisionId = 'all') ORDER BY CASE WHEN upazilaId = :upazilaId THEN 1 WHEN districtId = :districtId THEN 2 WHEN divisionId = :divisionId THEN 3 ELSE 4 END, issuedAt DESC")
    List<DisasterUpdateEntity> getByHometown(String divisionId, String districtId, String upazilaId);

    @Query("SELECT * FROM disaster_updates WHERE isActive = 1 AND districtId = :districtId ORDER BY issuedAt DESC")
    List<DisasterUpdateEntity> getByDistrict(String districtId);

    @Query("SELECT * FROM disaster_updates WHERE isActive = 1 AND disasterType = :disasterType ORDER BY issuedAt DESC")
    List<DisasterUpdateEntity> getByDisasterType(String disasterType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DisasterUpdateEntity> updates);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DisasterUpdateEntity update);

    @Query("SELECT COUNT(*) FROM disaster_updates WHERE isActive = 1")
    int getActiveCount();

    @Query("DELETE FROM disaster_updates")
    void deleteAll();
}

