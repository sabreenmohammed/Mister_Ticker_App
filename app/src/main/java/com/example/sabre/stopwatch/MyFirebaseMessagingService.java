package com.example.sabre.stopwatch;

import android.app.Service;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Spinner;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sabre on 8/31/2017.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private DynamoDBMapper mapper;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private List<StopwatchTask> allTasks;

    private final String TAG = "SABZ";
    @Override
    public void onMessageReceived(RemoteMessage message) {
        MisterTicker.updateMapper(getApplicationContext());
        Map<String, String> dataMap = message.getData();
        Intent taskIntent = new Intent("com.example.sabre.RequestReceiver");
        String taskName = dataMap.get(MisterTicker.BundleLabels.TASK_NAME);
        String requestType = dataMap.get(MisterTicker.BundleLabels.REQUEST_TYPE);
        taskIntent.putExtra(MisterTicker.BundleLabels.REQUEST_TYPE, requestType);
        StopwatchTask requestedTask;
        if (requestType.equals(MisterTicker.StopwatchActions.START))
            requestedTask = new StopwatchTask(taskName);
        else if (requestType.equals(MisterTicker.StopwatchActions.PAUSE) ||
                requestType.equals(MisterTicker.StopwatchActions.RESUME) ||
                requestType.equals(MisterTicker.StopwatchActions.END) ||
                requestType.equals(MisterTicker.StopwatchActions.DISCARD))
            requestedTask = getRequestedTask(taskName, MisterTicker.getAllRunningTasksFromDatabase());
        else {
            throw new IllegalArgumentException("Request type " + requestType + " unknown."); //passed in requestType is illegal
        }

        taskIntent.putExtra(MisterTicker.BundleLabels.CURRENT_TASK, requestedTask);

        if (StopwatchActivity.isActive && StopwatchActivity.currentTask.equals(requestedTask)){
            if (requestedTask.equals(MisterTicker.StopwatchActions.START))
                throw new IllegalArgumentException("Tried to start an ongoing task" + taskName + ".");
            else
                sendBroadcast(taskIntent);
        } else{
            taskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(taskIntent);
        }
    }


    private StopwatchTask getRequestedTask(String taskName, List<StopwatchTask> allTasks){
        if (taskName == null)
            throw new IllegalArgumentException();
        if (taskName.equals("undefined"))
            throw new IllegalArgumentException();
        for (StopwatchTask task: allTasks){
            if (task.getTaskName().equals(taskName))
                return task;
        }
        throw new IllegalArgumentException("Task named " + taskName +" doesn't exist.");
    }

}
