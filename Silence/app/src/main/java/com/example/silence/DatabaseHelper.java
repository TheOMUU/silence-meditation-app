package com.example.silence;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

public class DatabaseHelper extends SQLiteOpenHelper {

    /* =====================================================
       DATABASE CONFIGURATION
    ===================================================== */

    private static final String DATABASE_NAME = "silence.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /* =====================================================
       DATABASE CREATION
    ===================================================== */

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT," +
                        "email TEXT UNIQUE," +
                        "password TEXT," +
                        "role TEXT," +
                        "banned INTEGER DEFAULT 0)"
        );

        db.execSQL(
                "INSERT INTO users(name,email,password,role) VALUES('Admin','admin@silence.com','admin123','admin')"
        );

        db.execSQL(
                "CREATE TABLE user_inputs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "email TEXT," +
                        "problem TEXT," +
                        "emotion TEXT," +
                        "flagged INTEGER DEFAULT 0)"
        );

        db.execSQL(
                "CREATE TABLE chat_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_email TEXT," +
                        "message TEXT," +
                        "emotion TEXT," +
                        "time DATETIME DEFAULT CURRENT_TIMESTAMP)"
        );
    }

    /* =====================================================
       DATABASE UPGRADE
    ===================================================== */

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS user_inputs");
        db.execSQL("DROP TABLE IF EXISTS chat_history");

        onCreate(db);
    }

    /* =====================================================
       USER AUTHENTICATION
    ===================================================== */

    public boolean registerUser(String name, String email, String password, String role) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);

        long result = db.insert("users", null, values);

        return result != -1;
    }

    public String loginUser(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT role,banned FROM users WHERE email=? AND password=?",
                new String[]{email, password}
        );

        if (cursor.moveToFirst()) {

            int banned = cursor.getInt(1);

            if (banned == 1) {
                cursor.close();
                return "banned";
            }

            String role = cursor.getString(0);
            cursor.close();

            return role;
        }

        cursor.close();
        return null;
    }

    /* =====================================================
       ADMIN STATS
    ===================================================== */

    public int getUserCount() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null);

        int count = 0;

        if(cursor.moveToFirst()){
            count = cursor.getInt(0);
        }

        cursor.close();

        return count;
    }

    public int getEmotionCount() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM user_inputs", null);

        int count = 0;

        if(cursor.moveToFirst()){
            count = cursor.getInt(0);
        }

        cursor.close();

        return count;
    }

    /* =====================================================
       USER MANAGEMENT
    ===================================================== */

    public Cursor getAllUsers() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id,name,email FROM users",
                null
        );
    }

    public void deleteUser(int id) {

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT email FROM users WHERE id=?",
                new String[]{String.valueOf(id)}
        );

        if (c.moveToFirst()) {

            String email = c.getString(0);

            // Prevent deleting main admin
            if(email.equals("admin@silence.com")){
                c.close();
                return;
            }

            db.delete("user_inputs", "email=?", new String[]{email});
            db.delete("chat_history", "user_email=?", new String[]{email});
        }

        c.close();

        db.delete("users", "id=?", new String[]{String.valueOf(id)});
    }

    /* =====================================================
       USER ACTIVITY TRACKING
    ===================================================== */

    public void insertActivity(String email, String problem, String emotion, int flagged) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("email", email);
        values.put("problem", problem);
        values.put("emotion", emotion);
        values.put("flagged", flagged);

        db.insert("user_inputs", null, values);
    }

    public Cursor getUserActivity() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT email,problem,emotion,flagged FROM user_inputs ORDER BY id DESC",
                null
        );
    }

    /* =====================================================
       CHAT HISTORY SYSTEM
    ===================================================== */

    public void saveChat(String email, String message, String emotion) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_email", email);
        values.put("message", message);
        values.put("emotion", emotion);

        db.insert("chat_history", null, values);
    }

    public Cursor getChatHistory(String email) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, message, emotion FROM chat_history WHERE user_email=? ORDER BY id DESC",
                new String[]{email}
        );
    }

    /* =====================================================
       ACCOUNT SETTINGS
    ===================================================== */

    public void updateName(String email, String name) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);

        db.update(
                "users",
                values,
                "email=?",
                new String[]{email}
        );
    }

    public void updatePassword(String email, String password) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("password", password);

        db.update(
                "users",
                values,
                "email=?",
                new String[]{email}
        );
    }

    public void clearHistory(String email) {

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(
                "chat_history",
                "user_email=?",
                new String[]{email}
        );
    }

    /* =====================================================
       ADMIN ACTIVITY DATA
    ===================================================== */

    public Cursor getAllActivity() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT email, problem, emotion FROM user_inputs ORDER BY id DESC",
                null
        );
    }

    public Cursor getHighRiskUsers() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT email, problem, emotion FROM user_inputs WHERE flagged = 1",
                null
        );
    }

    public int getUserSadnessCount(String email) {

        SQLiteDatabase db = this.getReadableDatabase();

        int count = 0;

        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM user_inputs WHERE email=? AND emotion='sadness'",
                new String[]{email}
        );

        if (c.moveToFirst()) {
            count = c.getInt(0);
        }

        c.close();

        return count;
    }
    public void clearChatHistory(String email){

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("chat_history", "user_email=?", new String[]{email});

    }

    public void deleteChat(int id){

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("chat_history","id=?",new String[]{String.valueOf(id)});

    }
}