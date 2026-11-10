package com.jibonshield.bd.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jibonshield.bd.database.entity.UserProfileEntity;

@Dao
public interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    UserProfileEntity getByUid(String uid);

    @Query("SELECT * FROM user_profiles LIMIT 1")
    UserProfileEntity getActiveProfile();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfileEntity profile);

    @Query("UPDATE user_profiles SET divisionId = :divisionId, districtId = :districtId, upazilaId = :upazilaId, hometownConfigured = 1, updatedAt = :updatedAt WHERE uid = :uid")
    void updateHometown(String uid, String divisionId, String districtId, String upazilaId, long updatedAt);

    @Query("DELETE FROM user_profiles")
    void deleteAll();
}

