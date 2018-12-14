package com.test.android.agromap.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface PolygonDao {

    @Insert
    long insertReport(PolygonData polygonData);

    @Query("SELECT * FROM polygon")
    List<PolygonData> loadReports();
}
