package de.karlkuebelschule.kugelmatikapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import de.karlkuebelschule.KugelmatikLibrary.Config;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private Timer timer = null;
    private final KugelmatikManager kugelmatikManager = new KugelmatikManager();
    private boolean sensorEnabled = false;

    // UI elements
    private EditText host;
    private Button connect;

    private TextView status;
    private TextView height;

    private Button moveTo0;
    private Button moveTo100;
    private Button moveTo6000;
    private Button sensor;

    private Button sub100;
    private Button add100;
    private Button sub1000;
    private Button add1000;

    private EditText heightEditText;
    private Button setButton;

    private Button stop;
    private Button home;

    // Fields for sensor orientation sensor
    private SensorManager mSensorManager;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        host = (EditText) findViewById(R.id.hostEditText);
        connect = (Button) findViewById(R.id.connectButton);

        status = (TextView) findViewById(R.id.statusText);
        height = (TextView) findViewById(R.id.heightText);

        moveTo0 = (Button) findViewById(R.id.moveTo0Button);
        moveTo100 = (Button) findViewById(R.id.moveTo100Button);
        moveTo6000 = (Button) findViewById(R.id.moveTo6000Button);
        sensor = (Button) findViewById(R.id.sensorButton);

        sub100 = (Button) findViewById(R.id.sub100Button);
        add100 = (Button) findViewById(R.id.add100Button);
        sub1000 = (Button) findViewById(R.id.sub1000Button);
        add1000 = (Button) findViewById(R.id.add1000Button);

        heightEditText = (EditText) findViewById(R.id.heightEditText);
        setButton = (Button) findViewById(R.id.setButton);

        stop = (Button) findViewById(R.id.stopButton);
        home = (Button) findViewById(R.id.homeButton);

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    synchronized (kugelmatikManager) {
                        if (kugelmatikManager.isLoaded())
                            kugelmatikManager.free();
                        else {
                            String hostText = host.getText().toString().trim();

                            if (hostText.length() > 0) {
                                if (hostText.equalsIgnoreCase("*"))
                                    kugelmatikManager.loadKugelmatik();
                                else
                                    kugelmatikManager.load(hostText);
                            }
                            runTick();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showError();
                }
                sensorEnabled = false;
                updateUI();
            }
        });

        setHeightButton(moveTo0, 0);
        setHeightButton(moveTo100, 100);
        setHeightButton(moveTo6000, 6000);

        setAddHeightButton(sub100, -100);
        setAddHeightButton(add100, 100);
        setAddHeightButton(sub1000, -1000);
        setAddHeightButton(add1000, 1000);

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String text = heightEditText.getText().toString().trim();
                    kugelmatikManager.setHeight(Integer.parseInt(text));
                }
                catch(Exception e) {
                    showError();
                    e.printStackTrace();
                }
            }
        });

        sensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorEnabled = !sensorEnabled;
                updateUI();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorEnabled = false;
                kugelmatikManager.sendStop();
                updateUI();
            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorEnabled = false;
                kugelmatikManager.sendHome();
                updateUI();
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        updateUI();
        runTick();
    }

    private void setHeightButton(Button button, final int height) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    kugelmatikManager.setHeight(height);
                } catch (Exception e) {
                    e.printStackTrace();
                    showError();
                }
            }
        });
    }

    private void setAddHeightButton(Button button, final int amount) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int newHeight = kugelmatikManager.getHeight() + amount;
                    if (newHeight < 0)
                        newHeight = 0;
                    else if (newHeight > Config.MaxHeight)
                        newHeight = Config.MaxHeight;

                    kugelmatikManager.setHeight(newHeight);
                }
                catch(Exception e) {
                    e.printStackTrace();
                    showError();
                }
            }
        });
    }

    private void showError() {
        try {
            Toast.makeText(this, R.string.internal_error, Toast.LENGTH_LONG).show();
            updateUI();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void invokeUpdateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    private synchronized void updateUI() {
        try {
            boolean isConnected = kugelmatikManager.isConnected();

            if (isConnected) {
                connect.setText(R.string.disconnect);
                status.setText(getString(R.string.connected, kugelmatikManager.getPing(), kugelmatikManager.getVersion(), kugelmatikManager.getError().name()));

                height.setVisibility(View.VISIBLE);
                height.setText(getString(R.string.current_height, kugelmatikManager.getHeight()));

                if (sensorEnabled)
                    sensor.setText(R.string.disable_sensor);
                else
                    sensor.setText(R.string.enable_sensor);
            } else {
                if (kugelmatikManager.isLoaded()) {
                    connect.setText(R.string.disconnect);
                    status.setText(R.string.connecting);
                } else {
                    connect.setText(R.string.connect);
                    status.setText(R.string.not_connected);
                }

                height.setVisibility(View.INVISIBLE);
                sensor.setText(R.string.enable_sensor);
            }

            boolean enableManual = !sensorEnabled && isConnected;
            moveTo0.setEnabled(enableManual);
            moveTo100.setEnabled(enableManual);
            moveTo6000.setEnabled(enableManual);
            sub100.setEnabled(enableManual);
            add100.setEnabled(enableManual);
            sub1000.setEnabled(enableManual);
            add1000.setEnabled(enableManual);
            heightEditText.setEnabled(enableManual);
            setButton.setEnabled(enableManual);
            sensor.setEnabled(isConnected);
            stop.setEnabled(kugelmatikManager.isLoaded());
            home.setEnabled(kugelmatikManager.isLoaded());
        }
        catch(Exception e) {
            e.printStackTrace();
            showError();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,  mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (kugelmatikManager != null)
            kugelmatikManager.free();
    }

    private synchronized void runTick() {
        if (timer != null)
            timer.cancel();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    kugelmatikManager.sendPing();

                    if (sensorEnabled) {
                        synchronized (mOrientationAngles) {
                            float pi = (float) Math.PI;
                            float angle = mOrientationAngles[1];
                            float value = (angle + 0.5f * pi) / pi;

                            kugelmatikManager.setHeightPercentage(value);
                        }
                    }

                    invokeUpdateUI();
                }
                catch(Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showError();
                        }
                    });
                }
            }
        }, 0, 200);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mAccelerometerReading,
                        0, mAccelerometerReading.length);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagnetometerReading,
                        0, mMagnetometerReading.length);
                break;
        }
        updateOrientationAngles();
    }

    public synchronized void updateOrientationAngles() {
        synchronized (mOrientationAngles) {
            SensorManager.getRotationMatrix(mRotationMatrix, null,
                    mAccelerometerReading, mMagnetometerReading);
            SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
        }
    }
}
