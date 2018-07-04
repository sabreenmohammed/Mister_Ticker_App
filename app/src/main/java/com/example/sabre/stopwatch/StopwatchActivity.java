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
import java.util.MissingFormatArgumentException;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.iid.FirebaseInstanceId;

public class StopwatchActivity extends Activity{
    public static boolean isActive = false;
    private int totalSeconds;
    private Intent sourceIntent;
    public static StopwatchTask currentTask;


    StopwatchTask taskToDelete;
    PieChart chart;
    PieEntry thisTask;
    PieEntry otherTasks;
    PieData pieData;
    boolean chartIsVisible;

    private Button resumePauseButton;
    private TextView taskNameTextView;
    private Button editChecker;
    private EditText taskNameTextViewEditor;
    private Button doneButton;
    private BroadcastReceiver requestReceiver;


    private static final String TAG = "SABZ";

    @Override
    protected void onCreate(Bundle sourceBundle){
        super.onCreate(sourceBundle);
        MisterTicker.updateMapper(getApplicationContext());
        setupReceivers();
        isActive = true;
        setContentView(R.layout.activity_stopwatch);
        sourceIntent = getIntent();
        assignButtons();
        if (sourceBundle == null) //loaded from Firebase or InfoGatheringActivity
            sourceBundle = sourceIntent.getExtras();
        currentTask = sourceBundle.getParcelable(MisterTicker.BundleLabels.CURRENT_TASK);
        String requestType = sourceBundle.getString(MisterTicker.BundleLabels.REQUEST_TYPE);
        matchAndDispatch(requestType);
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        loadTaskToScreen();
        initializePie();
        runTimer();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(requestReceiver);
        isActive = false;
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putParcelable("currentTask", currentTask);
        savedInstanceState.putString("requestType", "continue");
    }

    //buttons
    public void onClickBack(View view){
        goBackToInfoGatheringActivity();
    }
    public void onClickTaskName(View view){
        taskNameTextViewEditor.setText(currentTask.getTaskName());
        taskNameTextView.setVisibility(View.INVISIBLE);
        taskNameTextViewEditor.setVisibility(View.VISIBLE);
        editChecker.setVisibility(View.VISIBLE);
    }
    public void onClickCheckButton(View view){
        currentTask.setTaskName(taskNameTextViewEditor.getText().toString());
        taskNameTextView.setText(currentTask.getTaskName());
        taskNameTextViewEditor.setVisibility(View.INVISIBLE);
        taskNameTextView.setVisibility(View.VISIBLE);
        editChecker.setVisibility(View.INVISIBLE);
    }
    public void onClickResumePause(View view){
        invokeResumePause();
    }
    public void onClickDone(View view){
        invokeEnd();
    }
    public void onClickDiscard(View view){
        invokeDiscard();
    }
    public void onClickGetVisual(View view) {
//        Intent visualizeIntent = new Intent(this, StopwatchVisualActivity.class);
//        startActivity(visualizeIntent);
        chartIsVisible = !chartIsVisible;
        if (chart.getVisibility() == View.VISIBLE)
            chart.setVisibility(View.INVISIBLE);
        else
            chart.setVisibility(View.INVISIBLE);
    }

    //------------private helper methods-------------------------------------
    private void invokeStart(){
        currentTask.startTask(Calendar.getInstance().getTimeInMillis());
        MisterTicker.addCurrentTaskToDatabase();
    }
    private void invokeResumePause(){
        currentTask.pauseTask(Calendar.getInstance().getTimeInMillis());
        if (currentTask.getIsRunning()) {
            resumePauseButton.setText("PAUSE");
        } else{
            resumePauseButton.setText("RESUME");
        }
        MisterTicker.addCurrentTaskToDatabase();
    }
    private void invokeEnd(){
        currentTask.endTask(Calendar.getInstance().getTimeInMillis());
        doneButton.setVisibility(View.GONE);
        resumePauseButton.setVisibility(View.GONE);
        taskNameTextView.setText("FINISHED: " + currentTask.getTaskName());
        MisterTicker.deleteTaskFromDatabase(MisterTicker.getTaskFromDatabase(0, currentTask.getInitializeTime()));
        MisterTicker.addCurrentTaskToDatabase();
    }
    private void invokeDiscard(){
        MisterTicker.deleteTaskFromDatabase(MisterTicker.getTaskFromDatabase(0, currentTask.getInitializeTime()));
        goBackToInfoGatheringActivity();
    }

    private void setupReceivers(){
        requestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle sourceBundle = intent.getExtras();
                String requestType = sourceBundle.getString("requestType");
                StopwatchTask receivedTask = sourceBundle.getParcelable("currentTask");
                if (currentTask.equals(receivedTask))
                    matchAndDispatch(requestType);
                else
                    finish();
            }
        };
        IntentFilter filter = new IntentFilter("com.example.sabre.RequestReceiver");
        registerReceiver(requestReceiver, filter);
    }
    private void runTimer(){
        final TextView timeView = (TextView)findViewById(R.id.time_view);
        final Handler handler = new Handler(); //Create a new Handler
        handler.post(new Runnable() { //Call the post() method, passing in a new runnable
            @Override                 //so the first time, the run() method is executed without delay
            public void run() {
                int hours = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;
                int secs = (totalSeconds % 3600) % 60;
                float taskPortion = (float)totalSeconds/60;
                String time = String.format("%d:%02d:%02d", hours, minutes, secs);
                timeView.setText(time);
                thisTask.setY(taskPortion);
                otherTasks.setY(1 - taskPortion);
                chart.setData(pieData);
                if (chartIsVisible){
                    chart.setVisibility(View.GONE);
                    chart.setVisibility(View.VISIBLE);  //TODO: fix visibility
                }

                if (currentTask.getIsRunning()) {
                    totalSeconds++;
                }
                handler.postDelayed(this, 1000); //think of this as recursive, keeps getting called
            }                                    //there is a delay of 1 second from here on out
        });
    }
    private void loadTaskToScreen(){
            taskNameTextView.setText(currentTask.getTaskName());
            totalSeconds = (int)(currentTask.getTotalMillisSpent(Calendar.getInstance().getTimeInMillis())/1000);
            if (currentTask.getIsRunning())
                resumePauseButton.setText("PAUSE");
            else
                resumePauseButton.setText("RESUME");
        }
    private void matchAndDispatch(String requestType){
        Log.d(TAG, "requestType: " + requestType);
        switch (requestType){
            case MisterTicker.StopwatchActions.START:
                invokeStart();
                break;
            case MisterTicker.StopwatchActions.RESUME:
                invokeResumePause();
                break;
            case MisterTicker.StopwatchActions.PAUSE:
                invokeResumePause();
                break;
            case MisterTicker.StopwatchActions.END:
                invokeEnd();
                break;
            case MisterTicker.StopwatchActions.DISCARD:
                invokeDiscard();
            case MisterTicker.StopwatchActions.CONTINUE:
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void assignButtons(){
        resumePauseButton = (Button)findViewById(R.id.resume_pause_button);
        taskNameTextView = (TextView)findViewById(R.id.task_name);
        taskNameTextViewEditor = (EditText)findViewById(R.id.task_name_editor);
        editChecker = (Button)findViewById(R.id.check_button);
        doneButton = (Button)findViewById(R.id.stop_button);
        chart = (PieChart) findViewById(R.id.chart);
    }

    private void goBackToInfoGatheringActivity(){
        Intent backIntent = new Intent(this, InfoGatheringActivity.class);
        startActivity(backIntent);
        finish();
    }
    private void initializePie(){
        List<PieEntry> entries = new ArrayList<>();
        thisTask = new PieEntry(0);
        otherTasks = new PieEntry(1);
        entries.add(thisTask);
        entries.add(otherTasks);
        PieDataSet dataSet = new PieDataSet(entries, "Label");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        pieData = new PieData(dataSet);
        chart.setData(pieData);
        chartIsVisible = false;
    }
}

