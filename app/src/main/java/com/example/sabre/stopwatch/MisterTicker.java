package com.example.sabre.stopwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by sabre on 9/8/2017.
 */

public class MisterTicker {
    private static StopwatchTask loadedTask;
    private static List<StopwatchTask> loadedrunningTasks;
    public static Quarter currentQuarter;
    public static Map<String, Quarter> quarters;
    public static DynamoDBMapper mapper;

    public MisterTicker(){
        quarters = new HashMap<>();
    }

    public static void addQuarter(long startDate, long endDate){
        Quarter newQuarter = new Quarter(startDate, endDate);
        quarters.put(newQuarter.getKey(), newQuarter);
    }
    public static void addCourse(String catalogName, String fullCourseName, int credits, int difficulty, String quarterKey){
        Quarter quarter = quarters.get(quarterKey);
        quarter.addCourse(new Course(catalogName, fullCourseName, credits, difficulty));
    }
    public static void addSyllabusCategory(String name, String type, double percentageOfGrade, String quarterKey, String courseKey){
        Quarter quarter = quarters.get(quarterKey);
        Course course = quarter.getCourses().get(courseKey);
        course.addSyllabusCategory(name, type, percentageOfGrade);
    }
    public static void addTask(){

    }
    public static boolean populateQuarterSpinner(Spinner quarterSpinner, Context context){
        List<String> quarterNames = new ArrayList<>();
        quarterNames.add(0, SelectMessages.SELECT_QUARTER);
        quarterNames.addAll(MisterTicker.quarters.keySet());
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, quarterNames);
        quarterSpinner.setAdapter(itemsAdapter);
        return true;
    }
    public static boolean populateCourseSpinner(Spinner courseSpinner, String quarterKey, Context context){
        if (!quarterKey.equals(SelectMessages.SELECT_QUARTER)) {
            Quarter selectedQuarter = quarters.get(quarterKey);
            List<String> courseNames = new ArrayList<>();
            courseNames.add(0, SelectMessages.SELECT_COURSE);
            courseNames.addAll(selectedQuarter.getCourses().keySet());
            ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, courseNames);
            courseSpinner.setAdapter(itemsAdapter);
            return true;
        }
        return false;
    }
    public static boolean populateSyllabusSpinner(Spinner syllabusSpinner, String courseKey, String quarterKey, Context context){
        if (!quarterKey.equals(SelectMessages.SELECT_QUARTER) && !courseKey.equals(SelectMessages.SELECT_COURSE)) {
            Quarter selectedQuarter = quarters.get(quarterKey);
            Course selectedCourse = selectedQuarter.getCourses().get(courseKey);
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add(0, SelectMessages.SELECT_SYLLABUS_CATEGORY);
            categoryNames.addAll(selectedCourse.getSyllabus().keySet());
            ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, categoryNames);
            syllabusSpinner.setAdapter(itemsAdapter);
            return true;
        }
        return false;
    }

    public static void refresh(View view){
        view.setVisibility(View.GONE);
        view.setVisibility(View.VISIBLE);
    }


    public static abstract class FutureTaskInfo {
        public List<String> notes;
        public long timeOfCreation;
    }
        public static class SchoolInfo extends FutureTaskInfo{
            private String courseKey;
            private String quarterKey;
            private String syllabusCategoryKey;
            private int pointValue;

            public SchoolInfo(){
            }

            public SchoolInfo(String courseKey, String quarterKey, String syllabusCategoryName, int pointValue){
                setCourseKey(courseKey);
                setSyllabusCategoryKey(syllabusCategoryName);
                setPointValue(pointValue);
            }

            public void setCourseKey(String courseKey){
                this.courseKey = courseKey;
            }

            public void setQuarterKey(String quarterKey){
                this.quarterKey = quarterKey;
            }

            public void setSyllabusCategoryKey(String syllabusCategoryKey){
                this.syllabusCategoryKey = syllabusCategoryKey;
            }
            public void setPointValue(int pointValue){
                this.pointValue = pointValue;
            }
        }
        public static class EventInfo extends FutureTaskInfo{
            //location stuff
            private long eventTime;
            private long duration;
            public EventInfo(long eventTime){
                this.eventTime = eventTime;
            }
        }
        public static class DeadlineInfo extends FutureTaskInfo{
            private long deadlineTime;
            private int hardness; //0 - no consequence, 4 - absolutely hard

            public DeadlineInfo(){
            }
            public DeadlineInfo(long deadlineTime, int hardness){
                setDeadlineTime(deadlineTime);
                setHardness(hardness);
            }

            public void setDeadlineTime(long deadlineTime){
                this.deadlineTime = deadlineTime;
            }

            public void setHardness(int hardness){
                this.hardness = hardness;
            }

        }

    public static class Quarter{
        private Map<String, Course> courses;
        public final long startDate; //TODO:figure out what is private and what is public
        public final long endDate;
        public Season quarterOfYear;

        public Quarter(long startDate, long endDate){
            this.startDate = startDate;
            this.endDate = endDate;
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTimeInMillis(startDate);
            if (startCalendar.get(Calendar.MONTH) < 3)
                quarterOfYear = Season.WINTER;
            else if (startCalendar.get(Calendar.MONTH)  < 5)
                quarterOfYear = Season.SPRING;
            else if (startCalendar.get(Calendar.MONTH)  < 8)
                quarterOfYear = Season.SUMMER;
            else
                quarterOfYear = Season.AUTUMN;
            courses = new HashMap<>();
        }

        public void addCourse(Course newCourse){
            courses.put(newCourse.getCatalogName(), newCourse);
        }

        public Map<String, Course> getCourses(){
            return courses;
        }

        public void deleteCourse(String courseName){
            //TODO:implement deleteCourse
        }
        public String getKey(){ //TODO:fix
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTimeInMillis(startDate);
            int year = startCalendar.get(Calendar.YEAR);
            return quarterOfYear + " " + year;
        }
    }
    public static class Course {
        private final String catalogName;
        private final String fullCourseName;
        private final int credits;
        private double perceivedDifficulty;
        private List<Long> meetingTimes;
        //TODO:add course location stuff
        private Map<String, SyllabusCategory> syllabus;
        private List<String> policies;

        public Course(String catalogName, String fullCourseName, int credits, int difficulty){
            this.catalogName = catalogName;
            this.fullCourseName = fullCourseName;
            this.credits = credits;
            this.perceivedDifficulty = difficulty;
            syllabus = new HashMap<>();
            policies = new ArrayList<>();
        }

        public String getCatalogName(){
            return catalogName;
        }
        public  void addSyllabusCategory(String name, String type, double percentageOfGrade){
            syllabus.put(name, new SyllabusCategory(name, type, percentageOfGrade));
        }
        public void addItem(SyllabusCategory syllabusCategory, StopwatchTask item){
            syllabusCategory.items.add(item);
        }
        public double getGrade(){
            return 0; //TODO: implement grade calculator
        }
        public Map<String, SyllabusCategory> getSyllabus(){
            return syllabus;
        }

        public class SyllabusCategory{
            private String name;
            private String type; //TODO: make enums - formal exam, informal exam, assignment, participation, lab
            private double percentageOfGrade;
            private List<StopwatchTask> items;
            private int estimatedTotalPoints;
            private int currentTotalPoints;
            private int currentAwardedPoints;
            private SyllabusCategory(String name, String type, double percentageOfGrade){
                this.name = name;
                this.type = type;
                this.percentageOfGrade = percentageOfGrade;
            }
        }
    }

    public enum Season{
        WINTER,SPRING, AUTUMN, SUMMER
    }

    public static class SelectMessages{
        public static final String SELECT_QUARTER = "Select quarter";
        public static final String SELECT_COURSE = "Select course";
        public static final String SELECT_SYLLABUS_CATEGORY = "Select category";
    }

    public static class TaskTypes{
        public static final String CHORE = "chore";
        public static final String EXAM = "exam";
        public static final String ASSIGNMENT = "assignment";
        public static final String EVENT = "event";
        public static final String LEARN = "learn";
    }

    public static class StopwatchActions{
        public static final String START = "start"; //midterms, finals
        public static final String RESUME = "resume"; //open-book, smaller (take home quizzes are assignments)
        public static final String PAUSE = "pause"; //attendance, but not a lab --clicker, etc.
        public static final String END = "end"; //requires attendance and procedure
        public static final String DISCARD = "discard"; //essays, take-home quizzes, no time limit
        public static final String CONTINUE = "continue";
    }
    public static class BundleLabels{
        public static final String CURRENT_TASK = "currentTask";
        public static final String REQUEST_TYPE = "requestType";
        public static final String TASK_NAME = "taskName";
    }

    private static CognitoCachingCredentialsProvider getCredentials(Context context){
         return new CognitoCachingCredentialsProvider(
                context,
                "us-east-1:06bef82f-3ba9-4655-998d-d17fe56f4e1e", // Identity pool ID
                Regions.US_EAST_1 // Region
        );
    }
    public static void updateMapper(Context context){
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(getCredentials(context));
        mapper = new DynamoDBMapper(ddbClient);
    }
    public static void addCurrentTaskToDatabase(){
        //String dateString = String.format("%1$tH:%1$tM:%1$tS on %1$tm/%1$te/%1$ty", c);
        Runnable runnable = new Runnable() {
            public void run() {
                mapper.save(StopwatchActivity.currentTask);
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }
    public static StopwatchTask getTaskFromDatabase(final long endTime, final long initializeTime){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                loadedTask = mapper.load(StopwatchTask.class, endTime, initializeTime);
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
        try {
            mythread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return loadedTask;
    }
    public static List<StopwatchTask> getAllRunningTasksFromDatabase(){
        final DynamoDBQueryExpression<StopwatchTask> queryExpression = new DynamoDBQueryExpression<StopwatchTask>()
                .withHashKeyValues(new StopwatchTask());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                loadedrunningTasks = mapper.query(StopwatchTask.class, queryExpression);

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
        try {
            mythread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return loadedrunningTasks;
    }
    public static void deleteTaskFromDatabase(final StopwatchTask taskToDelete){
        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                if (taskToDelete != null)
                    mapper.delete(taskToDelete);
            }
        };
        Thread mythread2 = new Thread(runnable2);
        mythread2.start();
        try {
            mythread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}


//    public enum SyllabusCategoryType{
//        FORMAL_EXAM ("formal exam"),
//        INFORMAL_EXAM ("informal exam"),
//        PARTICIPATION ("participation"),
//        LAB ("lab"),
//        ASSIGNMENT("assignment");
//
//        private final String stringName;
//
//        private SyllabusCategoryType(String stringName){
//            this.stringName = stringName;
//        }
//
//        public String getStringName(){
//            return stringName;
//        }
//    }
//
//    public enum TaskType{
//        CHORE ("chore"),
//        EXAM ("exam"),
//        ASSIGNMENT ("assignment"),
//        EVENT ("event"),
//        LEARN("learn");
//
//        private final String stringName;
//
//        private TaskType(String stringName){
//            this.stringName = stringName;
//        }
//
//        public String getStringName(){
//            return stringName;
//        }
//    }
//
//    public enum BundleLabel{
//        CURRENT_TASK ("currentTask"),
//        REQUEST_TYPE("requestType");
//
//        private final String stringName;
//
//        private BundleLabel(String stringName){
//            this.stringName = stringName;
//        }
//
//        public String getStringName(){
//            return stringName;
//        }
//    }
//
//    public enum StopwatchAction{
//        START ("start"),
//        RESUME ("resume"),
//        PAUSE ("pause"),
//        END ("end"),
//        DISCARD("discard"),
//        CONTINUE("continue");
//
//        private final String stringName;
//
//        private StopwatchAction(String stringName){
//            this.stringName = stringName;
//        }
//
//        public String getStringName(){
//            return stringName;
//        }
//    }
