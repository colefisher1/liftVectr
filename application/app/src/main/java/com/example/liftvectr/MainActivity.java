package com.example.liftvectr;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ederdoski.simpleble.models.BluetoothLE;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.liftvectr.data.Exercise;
import com.example.liftvectr.data.IMUData;
import com.example.liftvectr.database.ExerciseViewModel;
import com.example.liftvectr.util.BluetoothController;
import com.example.liftvectr.util.PermissionsHandler;
import com.example.liftvectr.util.ReadRunnable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String placeholderPair = "Please select a device to pair to";
    public static final String scanPair = "Select to rescan";

    private Button exerciseBtn;
    private Button viewChartBtn;
    private Spinner exerciseSpinner;
    private Spinner deviceListSpinner;
    private TextView x_accel, y_accel, z_accel;
    private TextView x_gyro, y_gyro, z_gyro;
    private TextView bluetoothConnected;

    private ArrayList<BluetoothLE> listDevices;
    private BluetoothController BLEController;

    private ExerciseViewModel exerciseViewModel;
    private List<Exercise> displayedExercises;

    private Exercise newExercise;

    private boolean exerciseOngoing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize and Assign Variable
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set Create Exercise Selected
        bottomNavigationView.setSelectedItemId(R.id.create);

        // Perform ItemSelectedListener
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.create:
                        return true;
                    case R.id.view:
                        startActivity(new Intent(getApplicationContext()
                            , ViewExerciseTest.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });

        exerciseViewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);

        // To clear the database on app startup, uncomment this!
        //exerciseViewModel.deleteAllExercises();

        exerciseBtn = (Button) findViewById(R.id.button);
        viewChartBtn = (Button) findViewById(R.id.view_chart_button);
        x_accel = (TextView) findViewById(R.id.x_a);
        y_accel = (TextView) findViewById(R.id.y_a);
        z_accel = (TextView) findViewById(R.id.z_a);
        x_gyro = (TextView) findViewById(R.id.x_g);
        y_gyro = (TextView) findViewById(R.id.y_g);
        z_gyro = (TextView) findViewById(R.id.z_g);
        bluetoothConnected = (TextView) findViewById(R.id.bluetooth_status);

        exerciseSpinner = (Spinner) findViewById(R.id.spinner);
        deviceListSpinner = (Spinner) findViewById(R.id.spinner2);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.exercises_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinners

        exerciseSpinner.setAdapter(adapter);

        bluetoothConnected.setText("Not Connected");

        // Reuqest Permissions
        PermissionsHandler.askForPermissions(this);

        // Initialize bluetooth controller
        BLEController = new BluetoothController(this);

        //Scan for devices and populate dropdown
        try {
            BLEController.scanDevices();
        } catch (Exception e) {
            e.printStackTrace();
        }

        deviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("deviceListSpinner: ", (String) deviceListSpinner.getSelectedItem());

                // For placeholder, do not try to pair
                if (deviceListSpinner.getSelectedItem() == MainActivity.placeholderPair || deviceListSpinner.getSelectedItem() == "" || deviceListSpinner.getSelectedItem() == null) {return;}
                else if (deviceListSpinner.getSelectedItem() == MainActivity.scanPair) {
                    try {
                        BLEController.scanDevices();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                BLEController.findAndPairMatchingDevice((String) deviceListSpinner.getSelectedItem());

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i("deviceListSpinner", "Nothing selected.");
            }
        });

        exerciseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(exerciseBtn.getText().equals("Start Exercise")) {
                    if(!BLEController.getPairedStatus()) {
                        setToastText("Need to pair to IMU first!");
                        return;
                    }
                    exerciseBtn.setText("Stop Exercise");
                    viewChartBtn.setVisibility(View.INVISIBLE);
                    exerciseOngoing = true;

                    newExercise = new Exercise(exerciseSpinner.getSelectedItem().toString(), Calendar.getInstance().getTime());

                    Runnable r = new ReadRunnable(BLEController, exerciseBtn);
                    Thread t = new Thread(r);
                    t.start();

                    /*// Fill exercise with fake bluetooth data
                    newExercise.addDataSample(new IMUData(0.2f, 1.0f, 0.43f, 0.0f, 0.1f, 0.0f, -9.0f, -1.0f, -1.0f, 1));
                    newExercise.addDataSample(new IMUData(0.23f, 0.95f, 0.41f, 0.3f, 0.2f, 0.3f, -8.4f,-1.0f, -0.8f, 2));
                    newExercise.addDataSample(new IMUData(0.28f, 1.10f, 0.39f, 0.5f, 0.1f, 0.5f, -9.3f,-0.9f, -1.0f, 3));
                    newExercise.addDataSample(new IMUData(0.25f, 1.03f, 0.43f, 0.3f, 0.3f, 0.9f, -7.2f,-1.0f, -0.9f, 4));
                    newExercise.addDataSample(new IMUData(0.29f, 0.93f, 0.45f, 0.0f, 0.2f, 1.1f, -6.3f,-0.9f, -1.0f, 5));
                    newExercise.addDataSample(new IMUData(0.24f, 0.98f, 0.49f, 0.3f, 0.1f, 3.3f, -6.8f,-1.0f, -1.3f, 6));
                    newExercise.addDataSample(new IMUData(0.22f, 1.01f, 0.46f, 0.5f, 0.0f, 3.0f, -9.5f,-0.8f, -1.0f, 7));
                    newExercise.addDataSample(new IMUData(0.21f, 1.03f, 0.42f, 0.3f, 0.0f, 2.1f, -10.0f,-1.0f, -1.2f, 8));
                    newExercise.addDataSample(new IMUData(0.24f, 0.94f, 0.40f, 0.0f, 0.1f, 1.2f, -9.1f, -0.9f, -1.0f, 9));
                    newExercise.addDataSample(new IMUData(0.26f, 0.99f, 0.43f, 0.3f, 0.2f, 0.4f, -9.3f,-1.2f, -1.1f, 10));*/
                }
                else {
                    // Save the exercise to the db
                    exerciseViewModel.saveExercise(newExercise);
                    exerciseBtn.setText("Start Exercise");
                    viewChartBtn.setVisibility(View.VISIBLE);
                    exerciseOngoing = false;
                }
            }

        });

        viewChartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chartConfig = "default";
                transitionToChartDisplayActivity(chartConfig);
            }
        });

        exerciseViewModel.getAllExercises().observe(this, exercises -> {
            System.out.println("An exercise has been added or deleted! Refresh the ui with the list of exercises here!");

            LiveData<List<Exercise>> savedExercises = exerciseViewModel.getAllExercises();
            System.out.println("Exercise List (Console Version): ");
            for (int i = 0; i < savedExercises.getValue().size(); i++) {
                System.out.print("Exercise Type:" + savedExercises.getValue().get(i).getType());
                System.out.println(", Exercise Date:" + savedExercises.getValue().get(i).getDate());
            }
        });
    }

    public void setBluetoothConnected(boolean value) {
        if(value) {
            this.bluetoothConnected.setText("Connected");
        }
        else {
           this.bluetoothConnected.setText("Not Connected");
        }
    }

    public void addDataToExercise(IMUData data) {
        if(this.newExercise != null) {
            newExercise.addDataSample(data);
        }
        else {
            Log.e("addDataToExercise", "Null newExercise.");
        }
    }

    public void setToastText(String Text) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, Text, Toast.LENGTH_SHORT).show());
    }

    public void setListDevices(ArrayList<BluetoothLE> list) {
        // This function should only be used by the BLE controller to maintain sync.
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceListSpinner.setAdapter(spinnerAdapter);

        // Add first placeholder button and rescan button
        spinnerAdapter.add(MainActivity.placeholderPair);
        spinnerAdapter.add(MainActivity.scanPair);
        // Could be null if we are resetting
        if (list != null) {
            for (BluetoothLE item : list) {
                if (item != null && item.getName() != null) {
                    spinnerAdapter.add(item.getName());
                } else {
                    Log.e("setListDevices", "null item name");
                }
            }
        }

        // Might be unnecessary;
        this.listDevices = list;
    }

    public void transitionToChartDisplayActivity(String config)
    {
        Intent intent = new Intent(this, ChartDisplay.class);
        intent.putExtra("exercise", newExercise);
        intent.putExtra("config", config);
        startActivity(intent);
    }

    public void displayData(IMUData sample) {
        x_accel.setText(Float.toString(sample.x_lin_acc));
        y_accel.setText(Float.toString(sample.y_lin_acc));
        z_accel.setText(Float.toString(sample.z_lin_acc));
        x_gyro.setText(Float.toString(sample.x_ang_vel));
        y_gyro.setText(Float.toString(sample.y_ang_vel));
        z_gyro.setText(Float.toString(sample.z_ang_vel));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}