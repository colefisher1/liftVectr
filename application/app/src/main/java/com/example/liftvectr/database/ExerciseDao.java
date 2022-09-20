package com.example.liftvectr.database;

import androidx.lifecycle.LiveData;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.liftvectr.data.Exercise;

import java.util.List;

// Defines methods for database access
@androidx.room.Dao
public interface ExerciseDao {
    @Insert
    void insert(Exercise exercise);

    @Delete
    void delete(Exercise exercise);

    @Query("DELETE FROM exercise_table")
    void deleteAllExercises();

    @Query("SELECT * FROM exercise_table ORDER BY date DESC")
    LiveData<List<Exercise>> getAllExercises();
}
