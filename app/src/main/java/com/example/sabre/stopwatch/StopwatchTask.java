package com.example.sabre.stopwatch;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sabre on 8/28/2017.
 */

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by sabre on 7/21/2017.
 */

@DynamoDBTable(tableName = "StopwatchTasks")
public class StopwatchTask implements Parcelable {
    private String taskName;
    private long initializeTime;
    private List<Long> breakTimes;
    private long endTime;
    private boolean running;

    private int taskType;
    private MisterTicker.DeadlineInfo deadlineInfo;
    private MisterTicker.SchoolInfo schoolInfo;
    private MisterTicker.EventInfo eventInfo;

    private long addedMillis;
    private long millisBeforeBreaks;

    //constructors
    public StopwatchTask(){
        initializeTime = 0;
        breakTimes = new ArrayList<Long>();
        addedMillis = 0;
        millisBeforeBreaks = 0;
        running = false;
    }

    public StopwatchTask(String taskName) {
        this();
        this.taskName = taskName;
    }
    public StopwatchTask(Parcel in) {
        taskName = in.readString(); //taskName
        initializeTime = in.readLong();
        int numberOfBreaks = in.readInt(); //number of breaks
        breakTimes = new ArrayList<>();
        for (int i = 0; i < numberOfBreaks; i++) { //all breaks
            breakTimes.add(in.readLong());
        }
        endTime = in.readLong();      //endTime
        millisBeforeBreaks = in.readLong();
        running = (in.readInt() == 1);
    }
    public StopwatchTask(MisterTicker.DeadlineInfo deadlineInfo, MisterTicker.SchoolInfo schoolInfo, MisterTicker.EventInfo eventInfo){ //TODO: add on extra parameter for assignment name
        this.deadlineInfo = deadlineInfo;
        this.schoolInfo = schoolInfo;
        this.eventInfo = eventInfo;
    }

    //DynamoDB tables

    @DynamoDBRangeKey(attributeName = "initializeTime")
    public long getInitializeTime(){
        return initializeTime;
    }
    public void setInitializeTime(long timeInMillis){initializeTime = timeInMillis;}


    @DynamoDBAttribute(attributeName = "taskName")
    public String getTaskName(){return taskName;
    }
    public void setTaskName(String taskName){
        this.taskName = taskName;
    }

    @DynamoDBAttribute(attributeName = "isRunning")
    public boolean getIsRunning(){return running;}
    public void setIsRunning(boolean running){this.running = running;}

    @DynamoDBHashKey(attributeName = "endTime")
    public long getEndTime(){
        return endTime;
    }
    public void setEndTime(long timeInMillis){this.endTime = timeInMillis;}

    @DynamoDBAttribute(attributeName = "breakTimes")
    public List<Long> getBreakTimes(){
        return breakTimes;
    }
    public void setBreakTimes(List<Long> timesInMillis){this.breakTimes = timesInMillis;}

    @DynamoDBAttribute(attributeName = "millisBeforeBreaks")
    public long getMillisBeforeBreaks(){
        return millisBeforeBreaks;
    }
    public void setMillisBeforeBreaks(long timeInMillis){this.millisBeforeBreaks = timeInMillis;}

    //public methods for activities
    public long getTotalMillisSpent(long timeInMillis) {
        updateAddedMillis(timeInMillis);
        return addedMillis + millisBeforeBreaks;
    }
    public void startTask(long timeInMillis){
        initializeTime = timeInMillis;
        addBreakTime(timeInMillis);
        running = true;
    }
    public void pauseTask(long timeInMillis){
        addBreakTime(timeInMillis);
        running = !running;
    }
    public void endTask(long timeInMillis) {
        endTime = timeInMillis;
        addBreakTime(timeInMillis);
        running = false;
    }
    public String toString(){
        double secondsSpent = (double) millisBeforeBreaks /1000;
        String out = "taskName: " + taskName + " " + secondsSpent + "\n";
        out+= breakTimes.toString();
        return out;
    }
    public boolean equals(StopwatchTask other){
        return (other != null && this.initializeTime == other.initializeTime && this.taskName.equals(other.getTaskName()));
    }

    //private helper methods
    private void updateAddedMillis(long timeInMillis){
        if (running){  //if timer running, updates addedMillis (time since last break) based on current time
            addedMillis = timeInMillis - breakTimes.get(breakTimes.size() - 1);
        }
    }
    public void addBreakTime(long timeInMillis) {
        if (running){ //was running prior to this call
            updateAddedMillis(timeInMillis);
            millisBeforeBreaks += addedMillis; //carries over, lumps in
            addedMillis = 0; //starts from new benchmark
        }
        breakTimes.add(timeInMillis); //adds break time
    }

    //Parcelable interface methods
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(taskName); //task name
        dest.writeLong(initializeTime); //initial time
        dest.writeInt(breakTimes.size()); //num breaks
        for (Long timeInMillis: breakTimes) {     //all breaks
            dest.writeLong(timeInMillis);
        }

        dest.writeLong(endTime); //end time
        dest.writeLong(millisBeforeBreaks); //total millis spent
        dest.writeInt(running ? 1 : 0);
    }
    public static final Parcelable.Creator<StopwatchTask> CREATOR =
            new Parcelable.Creator<StopwatchTask>() {
                public StopwatchTask createFromParcel(Parcel in) {
                    return new StopwatchTask(in);
                }

                public StopwatchTask[] newArray(int size) {
                    return new StopwatchTask[size];
                }
            };
}
