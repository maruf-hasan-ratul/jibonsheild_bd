package com.jibonshield.bd.database.converters;

import androidx.room.TypeConverter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringListConverter {

    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        JSONArray jsonArray = new JSONArray();
        for (String item : list) {
            jsonArray.put(item);
        }
        return jsonArray.toString();
    }

    @TypeConverter
    public static List<String> toList(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            JSONArray jsonArray = new JSONArray(value);
            List<String> list = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
            return list;
        } catch (JSONException e) {
            // Fallback: comma separated
            String[] parts = value.split(",");
            List<String> list = new ArrayList<>();
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    list.add(part.trim());
                }
            }
            return list;
        }
    }
}

