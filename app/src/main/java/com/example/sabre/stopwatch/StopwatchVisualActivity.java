package com.example.sabre.stopwatch;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.iid.FirebaseInstanceId;

public class StopwatchVisualActivity extends Activity{
    @Override
    protected void onCreate(Bundle sourceBundle){
        super.onCreate(sourceBundle);
        setContentView(R.layout.activity_stopwatch_visual);
        PieChart chart = (PieChart) findViewById(R.id.chart);
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) .4));
        entries.add(new PieEntry((float) .3));
        entries.add(new PieEntry((float) .1));
        PieDataSet dataSet = new PieDataSet(entries, "Label");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
    }

}


