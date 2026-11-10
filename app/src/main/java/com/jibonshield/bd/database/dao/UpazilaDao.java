package com.jibonshield.bd.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jibonshield.bd.database.entity.UpazilaEntity;

import java.util.List;

@Dao
public interface UpazilaDao {

    @Query("SELECT * FROM upazilas WHERE isActive = 1 ORDER BY name ASC")
    List<UpazilaEntity> getAllActive();

    @Query("SELECT * FROM upazilas ORDER BY name ASC")
    List<UpazilaEntity> getAll();

    @Query("SELECT * FROM upazilas WHERE districtId = :districtId AND isActive = 1 ORDER BY name ASC")
    List<UpazilaEntity> getByDistrictId(String districtId);

    @Query("SELECT * FROM upazilas WHERE id = :id LIMIT 1")
    UpazilaEntity getById(String id);

    @Query("SELECT * FROM upazilas WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    UpazilaEntity getByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UpazilaEntity> upazilas);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UpazilaEntity upazila);

    @Query("SELECT COUNT(*) FROM upazilas")
    int getCount();

    @Query("DELETE FROM upazilas")
    void deleteAll();
}

