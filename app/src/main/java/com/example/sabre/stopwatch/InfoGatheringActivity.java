package com.example.sabre.stopwatch;
import android.app.Activity;
import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//Main activity, where the app opens
public class InfoGatheringActivity extends Activity{
    private DynamoDBMapper mapper;
    private CognitoCachingCredentialsProvider credentialsProvider;

    Map<String, StopwatchTask> nameToTaskMapper;
    private Button resumeButton;
    private Spinner resumeTasksSpinner;
    private String TAG = "SABZLOG";
    private EditText messageView;
    private EditText futureMessageView;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_gathering);
        assignButtons();
        MisterTicker.updateMapper(getApplicationContext());
        prepareAllRunningItems();
    }
    public void onClickStart(View view){
        String messageText = (String)messageView.getText().toString();
        StopwatchTask stopwatchTask = new StopwatchTask(messageText);
        sendTaskToStopwatch(stopwatchTask, "start");
    }
    public void onClickResume(View view){
        String taskName = (String)resumeTasksSpinner.getSelectedItem();
        StopwatchTask stopwatchTask = nameToTaskMapper.get(taskName);
        sendTaskToStopwatch(stopwatchTask, "continue");
    }
    public void onClickAdd(View view){
        String messageText = (String)futureMessageView.getText().toString();
        StopwatchTask stopwatchTask = new StopwatchTask(messageText);
        sendTaskToGetMoreInfo(stopwatchTask);
    }

    public void onClickSettings(View view){
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        finish();
    }
    public void sendTaskToGetMoreInfo(StopwatchTask stopwatchTask){
        Intent addIntent = new Intent(this, MoreInfoGatheringActivity.class);
        addIntent.putExtra(MisterTicker.BundleLabels.CURRENT_TASK, stopwatchTask);
        startActivity(addIntent);
        finish();
    }
    private void sendTaskToStopwatch(StopwatchTask stopwatchTask, String requestType){
        Intent resumeIntent = new Intent(this, StopwatchActivity.class);
        resumeIntent.putExtra(MisterTicker.BundleLabels.CURRENT_TASK, stopwatchTask);
        resumeIntent.putExtra(MisterTicker.BundleLabels.REQUEST_TYPE, requestType);
        startActivity(resumeIntent);
        finish();
    }
    private void assignButtons(){
        resumeTasksSpinner = (Spinner)findViewById(R.id.resumeTasksSpinner);
        resumeButton = (Button)findViewById(R.id.resume);
        messageView = (EditText)findViewById(R.id.message);
        futureMessageView = (EditText)findViewById(R.id.message_future);
    }
    private void prepareAllRunningItems(){ //TODO: switch to a SQL database and DON'T do a full scan, use proper queries
        List<String> taskNames = new ArrayList<String>();
        List<StopwatchTask> runningTasks = MisterTicker.getAllRunningTasksFromDatabase();
        nameToTaskMapper = new HashMap<String, StopwatchTask>();
        for (StopwatchTask runningTask : runningTasks){
            taskNames.add(runningTask.getTaskName());
            nameToTaskMapper.put(runningTask.getTaskName(), runningTask);
        }

        if (taskNames.size() == 0)
            resumeButton.setEnabled(false);

        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, taskNames);
        resumeTasksSpinner.setAdapter(itemsAdapter); //TODO: fix if a null object is chosen so something null doesnt show up in database!!
    }

}