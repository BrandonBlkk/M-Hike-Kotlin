package com.example.hikermanagementapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HikeDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "HikerApp.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_HIKES = "hikes"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_DATE = "date"
        const val COLUMN_PARKING = "parking"
        const val COLUMN_LENGTH = "length"
        const val COLUMN_ROUTE_TYPE = "route_type"
        const val COLUMN_DIFFICULTY = "difficulty"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_NOTES = "notes"
        const val COLUMN_WEATHER = "weather"
        const val COLUMN_IS_COMPLETED = "is_completed"
        const val COLUMN_COMPLETED_DATE = "completed_date"
        const val COLUMN_CREATED_AT = "created_at"

        const val TABLE_OBSERVATIONS = "observations"
        const val COLUMN_OBS_ID = "obs_id"
        const val COLUMN_HIKE_ID = "hike_id"
        const val COLUMN_OBSERVATION = "observation"
        const val COLUMN_OBS_TIME = "obs_time"
        const val COLUMN_COMMENTS = "comments"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createHikesTable = """
            CREATE TABLE $TABLE_HIKES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_LOCATION TEXT NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_PARKING TEXT NOT NULL,
                $COLUMN_LENGTH REAL NOT NULL,
                $COLUMN_ROUTE_TYPE TEXT,
                $COLUMN_DIFFICULTY TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_NOTES TEXT,
                $COLUMN_WEATHER TEXT,
                $COLUMN_IS_COMPLETED INTEGER DEFAULT 0,
                $COLUMN_COMPLETED_DATE TEXT,
                $COLUMN_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        val createObservationsTable = """
            CREATE TABLE $TABLE_OBSERVATIONS (
                $COLUMN_OBS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HIKE_ID INTEGER NOT NULL,
                $COLUMN_OBSERVATION TEXT NOT NULL,
                $COLUMN_OBS_TIME TEXT NOT NULL,
                $COLUMN_COMMENTS TEXT,
                FOREIGN KEY($COLUMN_HIKE_ID) REFERENCES $TABLE_HIKES($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createHikesTable)
        db.execSQL(createObservationsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OBSERVATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HIKES")
        onCreate(db)
    }

    fun insertHikeWithExtras(hike: Hike, notes: String?, isCompleted: Int, completedDate: String?): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, hike.name)
            put(COLUMN_LOCATION, hike.location)
            put(COLUMN_DATE, hike.date)
            put(COLUMN_PARKING, hike.parking)
            put(COLUMN_LENGTH, hike.length)
            put(COLUMN_ROUTE_TYPE, hike.routeType)
            put(COLUMN_DIFFICULTY, hike.difficulty)
            put(COLUMN_DESCRIPTION, hike.description)
            put(COLUMN_NOTES, notes)
            put(COLUMN_WEATHER, hike.weather)
            put(COLUMN_IS_COMPLETED, isCompleted)
            put(COLUMN_COMPLETED_DATE, completedDate)
        }
        return db.insert(TABLE_HIKES, null, values)
    }

    fun getAllHikes(): List<Hike> {
        val hikes = mutableListOf<Hike>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HIKES,
            null, null, null, null, null,
            "$COLUMN_ID DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val hike = Hike(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        location = it.getString(it.getColumnIndexOrThrow(COLUMN_LOCATION)),
                        date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE)),
                        parking = it.getString(it.getColumnIndexOrThrow(COLUMN_PARKING)),
                        length = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LENGTH)),
                        routeType = it.getString(it.getColumnIndexOrThrow(COLUMN_ROUTE_TYPE)),
                        difficulty = it.getString(it.getColumnIndexOrThrow(COLUMN_DIFFICULTY)),
                        description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        notes = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTES)),
                        weather = it.getString(it.getColumnIndexOrThrow(COLUMN_WEATHER)),
                        isCompleted = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)),
                        completedDate = it.getString(it.getColumnIndexOrThrow(COLUMN_COMPLETED_DATE)),
                        createdAt = it.getString(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                    )
                    hikes.add(hike)
                } while (it.moveToNext())
            }
        }
        return hikes
    }

    fun deleteHike(hikeId: Long): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_HIKES, "$COLUMN_ID=?", arrayOf(hikeId.toString()))
        db.close()
        return result > 0
    }

    fun insertObservation(observation: Observation): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_HIKE_ID, observation.hikeId)
            put(COLUMN_OBSERVATION, observation.observation)
            put(COLUMN_OBS_TIME, observation.obsTime)
            put(COLUMN_COMMENTS, observation.comments)
        }
        return db.insert(TABLE_OBSERVATIONS, null, values)
    }

    fun getObservationsForHike(hikeId: Long): List<Observation> {
        val observations = mutableListOf<Observation>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_OBSERVATIONS,
            null,
            "$COLUMN_HIKE_ID=?",
            arrayOf(hikeId.toString()),
            null,
            null,
            "$COLUMN_OBS_ID DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                val obs = Observation(
                    obsId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_OBS_ID)),
                    hikeId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HIKE_ID)),
                    observation = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OBSERVATION)),
                    obsTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OBS_TIME)),
                    comments = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENTS))
                )
                observations.add(obs)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return observations
    }

    fun deleteObservation(obsId: Long) {
        val db = writableDatabase
        db.delete(TABLE_OBSERVATIONS, "$COLUMN_OBS_ID=?", arrayOf(obsId.toString()))
    }

    fun resetDatabase() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_OBSERVATIONS")
        db.execSQL("DELETE FROM $TABLE_HIKES")
    }
}