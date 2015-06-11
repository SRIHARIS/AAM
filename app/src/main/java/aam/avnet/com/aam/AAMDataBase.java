package aam.avnet.com.aam;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AAMDataBase extends SQLiteOpenHelper {

    private static final String TAG = "AAMDatabase";
    private static final String DATABASE_NAME = "aam.db";
    private static final int DATABASE_VERSION = 1;
    private final Context mContext;

    public AAMDataBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    interface Tables{
        //base column
        String ID = "id";
        //table name
        String VEHICLE = "vehicle";
        //columns
        String USERNAME = "username";
        String MANUFACTURER = "manufacturer";
        String VEHICLE_MODEL = "vehicle_model";
        String TORQUE = "torque";
        String COOLANT_TEMP = "coolant_temperature";
        String RPM = "rpm";
        String IGNITION_TIME = "ignitionTime";
        String SPEED = "speed";

        //table name
        String CAR_MILEAGE = "car_mileage";
        String CAR_ACCELERATION = "car_acceleration";
        String MILEAGE = "mileage";
        String ACCELERATION = "acceleration";
        String FUEL_CONSUMPTION = "fuel";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "+ Tables.VEHICLE + " ( "
            + Tables.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Tables.USERNAME + " TEXT NOT NULL"
            + Tables.MANUFACTURER + " TEXT NOT NULL"
            + Tables.VEHICLE_MODEL + " TEXT NOT NULL"
            + Tables.TORQUE + " INTEGER NOT NULL"
            + Tables.COOLANT_TEMP + " INTEGER NOT NULL"
            + Tables.RPM + " INTEGER NOT NULL"
            + Tables.IGNITION_TIME + " INTEGER NOT NULL"
            + Tables.SPEED + " INTEGER NOT NULL");

        db.execSQL("CREATE TABLE " + Tables.CAR_MILEAGE + " ( "
            + Tables.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Tables.USERNAME + " TEXT NOT NULL"
            + Tables.MANUFACTURER + " TEXT NOT NULL"
            + Tables.VEHICLE_MODEL + " TEXT NOT NULL"
            + Tables.MILEAGE + " REAL NOT NULL");

        db.execSQL("CREATE TABLE " + Tables.CAR_ACCELERATION + " ( "
            + Tables.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Tables.USERNAME + " TEXT NOT NULL"
            + Tables.MANUFACTURER + " TEXT NOT NULL"
            + Tables.VEHICLE_MODEL + " TEXT NOT NULL"
            + Tables.ACCELERATION + " REAL NOT NULL");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addVehicleData(ContentValues values){
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(Tables.VEHICLE, null, values);
    }

    public void addVehicleMileage(ContentValues values){
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(Tables.CAR_MILEAGE, null, values);
    }

    public void addVehicleAcceleration(ContentValues values){
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(Tables.CAR_ACCELERATION, null, values);
    }

    public void getVehicleData(){
        SQLiteDatabase db = this.getReadableDatabase();
        String []a = {
               Tables.USERNAME,
                Tables.SPEED
        };
        Cursor cursor = db.query(Tables.VEHICLE,a,null,null,null,null,null);
    }
}
