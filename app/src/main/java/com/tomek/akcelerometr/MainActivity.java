package com.tomek.akcelerometr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;


public class MainActivity extends Activity implements SensorEventListener { //implements Sensor... - czujnik

    private SensorManager mySensorManager; //menedzer czujnikow
    private Sensor myAccelerometer; //obiekt klasy Sensor (czyli czujnik)
    private boolean pomiar = false; //czy nacisnieto przycisk Start?
    private boolean marker = false;
    private int probki = 0;

    private TextView poleAx, poleAy, poleAz, poleAt, poleLK;
    private String string = "T:" + "\t" + "X:" + "\t" + "Y:" + "\t" + "Z:" + "\n"; //naglowek do txt
    private int licznik = 0;
    private PowerManager.WakeLock mWakeLock;

    private float aX;
    private float aY;
    private float aZ;
    private double aT;
    private double NS2S = 0.000000001; //do konwersji nanosekund do sekund
    private double startTime;
    private double timeStop;

    // definicje serii
    private XYSeries xTSeria = new XYSeries("X(t)");
    private XYSeries yTSeria = new XYSeries("Y(t)");
    private XYSeries zTSeria = new XYSeries("Z(t)");


    private XYSeriesRenderer rendererXT;

    private XYSeriesRenderer rendererYT;

    private XYSeriesRenderer rendererZT;

    private XYMultipleSeriesRenderer mrenderer;

    private XYMultipleSeriesDataset dataset;

    private LinearLayout chartLayout;
    private GraphicalView chartView;

// algorytm liczenia krokow - przechowywanie wart. przysp
    private float xAccel;
    private float yAccel;
    private float zAccel;

//algorytm lcizenia krokow - wczesniejsze wawrtosci przysp
    private float oldAX;
    private float oldAY;
    private float oldAZ;
    private boolean firstUpdate = true; //true = pierwsza zmiana przyspieszenia
    private final float shakeThreshold = 1.5f; //próg klasyfikacji jako wstrząs
    private boolean shakeInitiated = false; //czy wstrzas zostal rozpoczety (ruch w jednym kierunku)

    private int counter;

    private int czestotliwosc = 20;  //10 Hz

    Button buttonStart, buttonStop, buttonSave, buttonReset;

    PowerManager pm;

    NumberFormat numberFormat = new DecimalFormat("0.000000"); //format wyswietlania liczb na ekranie + wartosci czasu w txt
    NumberFormat numberFormat2 = new DecimalFormat("0.0000000000"); //format wyswietlania wartosci XYZ w txt


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //sensor
        mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myAccelerometer = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mySensorManager.registerListener(this, myAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        //umozliwienie dzialania programu pomimo zgaszonego ekranu
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

        buttonStart = (Button) findViewById(R.id.btnStart);
        buttonStop = (Button) findViewById(R.id.btnStop);
        buttonSave = (Button) findViewById(R.id.btnSave);
        buttonReset = (Button) findViewById(R.id.btnReset);

        buttonStop.setVisibility(View.GONE);
        buttonSave.setVisibility(View.GONE);
        buttonReset.setVisibility(View.GONE);
        buttonStart.setVisibility(View.VISIBLE);

        //przygotowanie do zachowania funkcjonalnosci aplikacji po obrocie urzadzenia

        if (savedInstanceState != null) {
            aX = savedInstanceState.getFloat("aX");
            aY = savedInstanceState.getFloat("aY");
            aZ = savedInstanceState.getFloat("aZ");
            aT = savedInstanceState.getFloat("aT");
            licznik = savedInstanceState.getInt("licznik");
            pomiar = savedInstanceState.getBoolean("pomiar");
            marker = savedInstanceState.getBoolean("marker");
            string = savedInstanceState.getString("string");
            startTime = savedInstanceState.getDouble("startTime");

        }

        if (marker) {
            buttonStart.setVisibility(View.GONE);
            buttonStop.setVisibility(View.VISIBLE);
            buttonSave.setVisibility(View.GONE);
            buttonReset.setVisibility(View.GONE);
            mWakeLock.acquire();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) { //gdy czujnik wykryje zmiane

        probki += 1;

        if (probki == czestotliwosc) {

            if (pomiar) {

                int sensorType = sensorEvent.sensor.getType();

                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) ;

                //if do ustalenia prawidlowego czasu - zapis pierwszego odczytu i od kolejnych odejmowanie jego wartosci - czas startuje dzieki temu od zera
                if (licznik == 0) {
                    startTime = sensorEvent.timestamp;
                }

                aX = sensorEvent.values[0]; //skladowa X przyspieszenia
                aY = sensorEvent.values[1]; //skladowa Y przyspieszenia
                aZ = sensorEvent.values[2]; //skladowa Z przyspieszenia

                aT = (sensorEvent.timestamp - startTime) * NS2S; //timestamp z konwersją na sekundy

                poleAx = (TextView) findViewById(R.id.poleAx);
                poleAy = (TextView) findViewById(R.id.poleAy);
                poleAz = (TextView) findViewById(R.id.poleAz);
                poleAt = (TextView) findViewById(R.id.poleAt);
                poleLK = (TextView) findViewById(R.id.poleLK);


                //wyswietlanie sformatowanych danych pomiarowych
                poleAx.setText(String.valueOf(numberFormat.format(aX)));
                poleAy.setText(String.valueOf(numberFormat.format(aY)));
                poleAz.setText(String.valueOf(numberFormat.format(aZ)));
                poleAt.setText(String.valueOf(numberFormat.format(aT)));

                //kolejne wiersze z parametrami - do zapisu w txt
                string = string + numberFormat.format(aT) + "\t" + numberFormat2.format(aX) + "\t" + numberFormat2.format(aY) + "\t" + numberFormat2.format(aZ) + "\n";

                // dodanie zmiennych do serii
                xTSeria.add(aT, aX);
                yTSeria.add(aT, aY);
                zTSeria.add(aT, aZ);


                licznik++;

                // umozliwienie wyswietlania wykresu "na biezaco"
                mrenderer.setXAxisMin(xTSeria.getMaxX() - 2);
                mrenderer.setXAxisMax(xTSeria.getMaxX());
                chartView.repaint();

// ALGORYTM POMIARU LICZBY KROKOW:
                updateAccelParameters(aX, aY, aZ);

                if ((!shakeInitiated) && isAccelerationChanged()) {
                    shakeInitiated = true;
                } else if ((shakeInitiated) && isAccelerationChanged()) {
                    executeShakeAction();

                } else if ((shakeInitiated) && (!isAccelerationChanged())) {
                    shakeInitiated = false;
                }

            }
            probki = 0;
        }
    }


    private void updateAccelParameters(float xNewAccel, float yNewAccel, float zNewAccel) {

        if (firstUpdate) {
            oldAX = xNewAccel;
            oldAY = yNewAccel;
            oldAZ = zNewAccel;
            firstUpdate = false;
        } else {
            oldAX = xAccel;
            oldAY = yAccel;
            oldAZ = zAccel;
        }
        xAccel = xNewAccel;
        yAccel = yNewAccel;
        zAccel = zNewAccel;

    }

    private boolean isAccelerationChanged() {
        float deltaX = Math.abs(oldAX - xAccel);
        float deltaY = Math.abs(oldAY - yAccel);
        float deltaZ = Math.abs(oldAZ - zAccel);
        return (deltaX > shakeThreshold && deltaY > shakeThreshold)
                || (deltaX > shakeThreshold && deltaZ > shakeThreshold)
                || (deltaY > shakeThreshold && deltaZ > shakeThreshold);
    }

    private void executeShakeAction() {

        counter++;
        poleLK.setText(String.valueOf(counter));

    }


//KONIEC ALGORYTMU


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { //jak na razie nie zwracamy na nia uwagi

    }

    //this - oznacza, ze uzywamy metod onSensorChanged oraz onAccuracyChanged
    //NS2S - stala, ktora ewentualnie mozna sobie zdefiniowac : 1*10^-9 (nanoseconds to seconds - czas z urzadzenia jest w ns)
    //w pkt 2 - umozliwianie pomiaru przyspieszen przy wylaczonym ekranie. Trzeba dodac .release(), gdyz jesli tego nie dodamy, to akcelerometr bedzie caly czas dzialal

    public void startWc(View view) {
        buttonStart.setVisibility(View.GONE);
        buttonStop.setVisibility(View.VISIBLE);
        buttonSave.setVisibility(View.GONE);
        buttonReset.setVisibility(View.GONE);
        pomiar = true;
        marker = true;

        counter = 0;
        mWakeLock.acquire();

        //dane do wykresu
        dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(xTSeria);
        dataset.addSeries(yTSeria);
        dataset.addSeries(zTSeria);


        // rendery dla kolejnych serii
        rendererXT = new XYSeriesRenderer();
        rendererXT.setLineWidth(2);
        rendererXT.setColor(Color.BLUE);
        rendererXT.setPointStyle(PointStyle.CIRCLE);

        rendererYT = new XYSeriesRenderer();
        rendererYT.setLineWidth(2);
        rendererYT.setColor(Color.RED);
        rendererYT.setPointStyle(PointStyle.CIRCLE);

        rendererZT = new XYSeriesRenderer();
        rendererZT.setLineWidth(2);
        rendererZT.setColor(Color.GREEN);
        rendererZT.setPointStyle(PointStyle.CIRCLE);

        //multiple series render, modyfikacje estetyczne
        mrenderer = new XYMultipleSeriesRenderer();
        mrenderer.addSeriesRenderer(rendererXT);
        mrenderer.addSeriesRenderer(rendererYT);
        mrenderer.addSeriesRenderer(rendererZT);
        mrenderer.setYAxisMax(25);
        mrenderer.setYAxisMin(-25);
        mrenderer.setShowGrid(true);
        mrenderer.setMarginsColor(Color.WHITE);
        mrenderer.setGridColor(Color.LTGRAY);
        mrenderer.setAxesColor(Color.BLACK);
        mrenderer.setXLabelsColor(Color.BLACK);
        mrenderer.setYLabelsColor(0, Color.BLACK);
        mrenderer.setYLabelsAlign(Paint.Align.RIGHT);
        mrenderer.setLabelsTextSize(15);
        mrenderer.setLegendTextSize(15);

        dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(xTSeria);
        dataset.addSeries(yTSeria);
        dataset.addSeries(zTSeria);

        chartLayout = (LinearLayout) findViewById(R.id.chart);
        chartView = ChartFactory.getLineChartView(this, dataset, mrenderer);
        chartLayout.addView(chartView);
        chartView.repaint();
    }


    public void stopWc(View view) {
        if (pomiar) {
            buttonStart.setVisibility(View.GONE);
            buttonStop.setVisibility(View.GONE);
            buttonSave.setVisibility(View.VISIBLE);
            buttonReset.setVisibility(View.VISIBLE);
            pomiar = false;
            timeStop = xTSeria.getMaxX();
            marker = true;
            mWakeLock.release();
        }
    }

    public void saveFile(View view) {
        zapiszPlik("/AKCELEROMETR/", "pomiar.txt");


    }

    private void zapiszPlik(String folder, String fileName) {


        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + folder); //tworzenie nowego pliku w nowym folderze
        dir.mkdirs();
        File file = new File(dir, fileName);

        String test = file.getAbsolutePath();
        Log.i("My", "FILE LOCATION: " + test);


        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);


            pw.print(string);

            pw.flush();
            pw.close();
            f.close();


            Toast.makeText(getApplicationContext(),

                    "Zapisano plik", //wiadomosc gdy zapis zakonczyl sie sukcesem

                    Toast.LENGTH_LONG).show();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("My", "File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the manifest?");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void reset(View view) {

        onCreate(new Bundle()); //na nowo metoda onCreate

        //czyszczenie danych dla kazdej serii
        xTSeria.clearSeriesValues();
        yTSeria.clearSeriesValues();
        zTSeria.clearSeriesValues();

        string = "T:" + "\t" + "X:" + "\t" + "Y:" + "\t" + "Z:" + "\n"; //zachowanie nagłówka do txt po resecie
    }


    //przygotowanie do zachowania funkcjonalnosci aplikacji po obrocie urzadzenia
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putFloat("aX", Float.parseFloat(String.valueOf(aX)));
        savedInstanceState.putFloat("aY", Float.parseFloat(String.valueOf(aY)));
        savedInstanceState.putFloat("aZ", Float.parseFloat(String.valueOf(aZ)));
        savedInstanceState.putFloat("aT", Float.parseFloat(String.valueOf(aT)));
        savedInstanceState.putInt("licznik", licznik);
        savedInstanceState.putBoolean("pomiar", pomiar);
        savedInstanceState.putBoolean("marker", marker);
        savedInstanceState.putString("string", string);
        savedInstanceState.putDouble("startTime", startTime);

    }

}

