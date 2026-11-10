package com.jibonshield.bd.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jibonshield.bd.database.entity.ShelterEntity;

import java.util.List;

@Dao
public interface ShelterDao {

    @Query("SELECT * FROM shelters WHERE isActive = 1 ORDER BY name ASC")
    List<ShelterEntity> getAllActive();

    @Query("SELECT * FROM shelters ORDER BY name ASC")
    List<ShelterEntity> getAll();

    @Query("SELECT * FROM shelters WHERE id = :id LIMIT 1")
    ShelterEntity getById(String id);

    @Query("SELECT * FROM shelters WHERE divisionId = :divisionId AND isActive = 1 ORDER BY name ASC")
    List<ShelterEntity> getByDivision(String divisionId);

    @Query("SELECT * FROM shelters WHERE districtId = :districtId AND isActive = 1 ORDER BY name ASC")
    List<ShelterEntity> getByDistrict(String districtId);

    @Query("SELECT * FROM shelters WHERE districtId = :districtId AND upazilaId = :upazilaId AND isActive = 1 ORDER BY name ASC")
    List<ShelterEntity> getByDistrictAndUpazila(String districtId, String upazilaId);

    @Query("SELECT * FROM shelters WHERE isActive = 1 AND (LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(nameBn) LIKE '%' || LOWER(:query) || '%' OR LOWER(address) LIKE '%' || LOWER(:query) || '%' OR LOWER(addressBn) LIKE '%' || LOWER(:query) || '%') ORDER BY name ASC")
    List<ShelterEntity> search(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ShelterEntity> shelters);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ShelterEntity shelter);

    @Query("SELECT COUNT(*) FROM shelters WHERE isActive = 1")
    int getActiveCount();

    @Query("SELECT COUNT(*) FROM shelters WHERE districtId = :districtId AND isActive = 1")
    int getCountByDistrict(String districtId);

    @Query("DELETE FROM shelters")
    void deleteAll();
}

