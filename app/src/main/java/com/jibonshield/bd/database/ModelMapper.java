package com.jibonshield.bd.database;

import com.jibonshield.bd.database.entity.DisasterUpdateEntity;
import com.jibonshield.bd.database.entity.ShelterEntity;
import com.jibonshield.bd.database.entity.UserProfileEntity;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.UserProfile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ModelMapper {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    private ModelMapper() {}

    public static Shelter toShelter(ShelterEntity entity) {
        if (entity == null) return null;
        long numericId = Math.abs((long) entity.getId().hashCode());
        String facilitiesStr = entity.getFacilities() != null ? String.join(", ", entity.getFacilities()) : "";
        return new Shelter(
                numericId,
                entity.getName(),
                entity.getNameBn(),
                entity.getDistrictId(),
                entity.getUpazilaId(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getCapacity(),
                entity.getCurrentOccupancy(),
                entity.getContactNumber(),
                entity.getShelterType(),
                facilitiesStr,
                "MoDMR / Firebase",
                entity.isActive(),
                DATE_FORMAT.format(new Date(entity.getUpdatedAt() > 0 ? entity.getUpdatedAt() : System.currentTimeMillis())),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static FloodWarning toFloodWarning(DisasterUpdateEntity entity) {
        if (entity == null) return null;
        long numericId = Math.abs((long) entity.getId().hashCode());
        String issued = entity.getIssuedAt() > 0 ? DATE_FORMAT.format(new Date(entity.getIssuedAt())) : "";
        String expires = entity.getExpiresAt() > 0 ? DATE_FORMAT.format(new Date(entity.getExpiresAt())) : "";
        String region = entity.getUpazilaId() != null && !entity.getUpazilaId().isEmpty()
                ? entity.getUpazilaId() + ", " + entity.getDistrictId()
                : (entity.getDistrictId() != null ? entity.getDistrictId() : "National");

        return new FloodWarning(
                numericId,
                entity.getSeverity() != null ? entity.getSeverity().toUpperCase() : "WARNING",
                entity.getTitle(),
                entity.getTitleBn(),
                region,
                entity.getDescription(),
                entity.getDescriptionBn(),
                issued,
                expires,
                entity.getIssuedBy() != null ? entity.getIssuedBy() : "Disaster Warning Center",
                entity.getId(),
                entity.isActive(),
                false,
                entity.getUpdatedAt()
        );
    }

    public static UserProfile toUserProfile(UserProfileEntity entity) {
        if (entity == null) return null;
        return new UserProfile(
                entity.getUid(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getDivisionId(),
                entity.getDistrictId(),
                entity.getUpazilaId(),
                entity.isHometownConfigured(),
                entity.getUpdatedAt()
        );
    }
}

