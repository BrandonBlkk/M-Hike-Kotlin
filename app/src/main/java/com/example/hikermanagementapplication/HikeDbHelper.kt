package com.example.hikermanagementapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HikeDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "HikerApp.db"
        private const val DATABASE_VERSION = 2 // ⬆️ Updated version since table changed

        const val TABLE_HIKES = "hikes"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_DATE = "date"
        const val COLUMN_PARKING = "parking"
        const val COLUMN_LENGTH = "length"
        const val COLUMN_DIFFICULTY = "difficulty"
        const val COLUMN_STATUS = "status" // 🆕 New column
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_TRAIL_TYPE = "trail_type"
        const val COLUMN_WEATHER = "weather"

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
                $COLUMN_DIFFICULTY TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_TRAIL_TYPE TEXT,
                $COLUMN_WEATHER TEXT
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

    fun insertHike(hike: Hike): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, hike.name)
            put(COLUMN_LOCATION, hike.location)
            put(COLUMN_DATE, hike.date)
            put(COLUMN_PARKING, hike.parking)
            put(COLUMN_LENGTH, hike.length)
            put(COLUMN_DIFFICULTY, hike.difficulty)
            put(COLUMN_STATUS, hike.status) // 🆕 Added status
            put(COLUMN_DESCRIPTION, hike.description)
            put(COLUMN_TRAIL_TYPE, hike.trailType)
            put(COLUMN_WEATHER, hike.weather)
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
                        difficulty = it.getString(it.getColumnIndexOrThrow(COLUMN_DIFFICULTY)),
                        status = it.getString(it.getColumnIndexOrThrow(COLUMN_STATUS)), // 🆕
                        description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        trailType = it.getString(it.getColumnIndexOrThrow(COLUMN_TRAIL_TYPE)),
                        weather = it.getString(it.getColumnIndexOrThrow(COLUMN_WEATHER))
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