package com.jibonshield.bd.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.jibonshield.bd.database.converters.StringListConverter;
import com.jibonshield.bd.database.dao.DisasterUpdateDao;
import com.jibonshield.bd.database.dao.DistrictDao;
import com.jibonshield.bd.database.dao.DivisionDao;
import com.jibonshield.bd.database.dao.ShelterDao;
import com.jibonshield.bd.database.dao.SyncMetadataDao;
import com.jibonshield.bd.database.dao.UpazilaDao;
import com.jibonshield.bd.database.dao.UserProfileDao;
import com.jibonshield.bd.database.entity.DisasterUpdateEntity;
import com.jibonshield.bd.database.entity.DistrictEntity;
import com.jibonshield.bd.database.entity.DivisionEntity;
import com.jibonshield.bd.database.entity.ShelterEntity;
import com.jibonshield.bd.database.entity.SyncMetadataEntity;
import com.jibonshield.bd.database.entity.UpazilaEntity;
import com.jibonshield.bd.database.entity.UserProfileEntity;

@Database(
    entities = {
        DivisionEntity.class,
        DistrictEntity.class,
        UpazilaEntity.class,
        ShelterEntity.class,
        DisasterUpdateEntity.class,
        UserProfileEntity.class,
        SyncMetadataEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({StringListConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "jibonshield_room.db";
    private static volatile AppDatabase INSTANCE;

    public abstract DivisionDao divisionDao();
    public abstract DistrictDao districtDao();
    public abstract UpazilaDao upazilaDao();
    public abstract ShelterDao shelterDao();
    public abstract DisasterUpdateDao disasterUpdateDao();
    public abstract UserProfileDao userProfileDao();
    public abstract SyncMetadataDao syncMetadataDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

