package com.example.sabre.stopwatch;
import android.app.Activity;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoreInfoGatheringActivity extends Activity{
    private DynamoDBMapper mapper;
    private CognitoCachingCredentialsProvider credentialsProvider;
    List<StopwatchTask> runningTasks;
    Map<String, StopwatchTask> nameToTaskMapper;
    private Button nextButton;
    private Spinner taskTypeSpinner;
    private ViewFlipper infoFlipper;
    private DatePicker deadlineDatePicker;
    private TimePicker deadlineTimePicker;
    private NumberPicker pointValuePicker;
    private SeekBar deadlineHardnessPicker;
    private Spinner courseSpinner;
    private Spinner quarterSpinner;
    private Spinner syllabusCategorySpinner;

    private Calendar deadlineCalendar;


    private RelativeLayout schoolView;
    private ViewGroup.LayoutParams schoolViewParams;
    private RelativeLayout deadlineView;
    private ViewGroup.LayoutParams deadlineViewParams;

    private MisterTicker.SchoolInfo schoolInfo;
    private MisterTicker.DeadlineInfo deadlineInfo;

    private TextView taskTypeMessage;
    private String TAG = "SABZLOG";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_info_gathering);
        assignButtons();
        schoolInfo = new MisterTicker.SchoolInfo();
        deadlineInfo = new MisterTicker.DeadlineInfo();
        deadlineCalendar = Calendar.getInstance();
        MisterTicker.populateQuarterSpinner(quarterSpinner, getBaseContext());
        taskTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView itemSelected = (TextView)view;
                String taskTypeChosen = itemSelected.getText().toString().trim();
                Log.d(TAG, MisterTicker.TaskTypes.ASSIGNMENT + " and " +taskTypeChosen);
                taskTypeMessage.setText(taskTypeChosen);
                    infoFlipper.removeAllViews();
                    switch (taskTypeChosen){
                    case MisterTicker.TaskTypes.ASSIGNMENT:
                        infoFlipper.addView(schoolView, schoolViewParams);
                        infoFlipper.addView(deadlineView, deadlineViewParams);
                        break;
                    case MisterTicker.TaskTypes.CHORE:
                        infoFlipper.addView(deadlineView, deadlineViewParams);
                        break;
                    case MisterTicker.TaskTypes.EVENT:
                        infoFlipper.addView(deadlineView, deadlineViewParams);
                        break;
                    case MisterTicker.TaskTypes.LEARN:
                        infoFlipper.addView(schoolView, schoolViewParams);
                        infoFlipper.addView(deadlineView, deadlineViewParams);
                        break;
                    case MisterTicker.TaskTypes.EXAM:
                        infoFlipper.addView(schoolView, schoolViewParams);
                        infoFlipper.addView(deadlineView, deadlineViewParams);
//
                        break;
                    default:
                        break;
                }
                infoFlipper.setVisibility(View.GONE);
                infoFlipper.setVisibility(View.VISIBLE);




            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        quarterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedQuarterKeyName = quarterSpinner.getSelectedItem().toString(); //TODO:fix redundancy
                boolean wasValidChoice = MisterTicker.populateCourseSpinner(courseSpinner, selectedQuarterKeyName, getBaseContext());
                MisterTicker.refresh(courseSpinner);
                MisterTicker.refresh(syllabusCategorySpinner);
                courseSpinner.setEnabled(wasValidChoice);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedQuarterKeyName = quarterSpinner.getSelectedItem().toString();
                String selectedCourseKeyName = courseSpinner.getSelectedItem().toString(); //TODO:fix redundancy
                boolean wasValidChoice = MisterTicker.populateSyllabusSpinner(syllabusCategorySpinner, selectedCourseKeyName, selectedQuarterKeyName, getBaseContext());
                MisterTicker.refresh(courseSpinner);
                syllabusCategorySpinner.setEnabled(wasValidChoice);
                schoolInfo.setCourseKey(selectedCourseKeyName);

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        syllabusCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategoryName = syllabusCategorySpinner.getSelectedItem().toString();
                schoolInfo.setSyllabusCategoryKey(selectedCategoryName);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void onClickDate(View view){
        int month = deadlineDatePicker.getMonth();
        int year = deadlineDatePicker.getYear();
        int date = deadlineDatePicker.getDayOfMonth();
        deadlineCalendar.set(year, month, date);
        deadlineInfo.setDeadlineTime(deadlineCalendar.getTimeInMillis());
    }

    public void onClickTime(View view){
        int month = deadlineDatePicker.getMonth();
        int year = deadlineDatePicker.getYear();
        int date = deadlineDatePicker.getDayOfMonth();
        int hour = deadlineTimePicker.getHour();
        int minute = deadlineTimePicker.getMinute();
        deadlineCalendar.set(year, month, date, hour, minute);
        deadlineInfo.setDeadlineTime(deadlineCalendar.getTimeInMillis());
    }

    public void onClickHardness(View view){
        int hardness = deadlineHardnessPicker.getProgress();
        deadlineInfo.setHardness(hardness);
    }

    public void onClickAdd(View view){
        StopwatchTask futureTask = new StopwatchTask(deadlineInfo, schoolInfo, null); //TODO: add eventInfo later
    }



    public void onClickNext(View view){
        infoFlipper.showNext();
    }
    private void assignButtons(){
        nextButton = (Button) findViewById(R.id.next);
        infoFlipper = (ViewFlipper)findViewById(R.id.Info);
        deadlineDatePicker = (DatePicker)findViewById(R.id.datePicker);
        deadlineTimePicker = (TimePicker)findViewById(R.id.timePicker);
        pointValuePicker = (NumberPicker)findViewById(R.id.point_value_picker);
        deadlineHardnessPicker = (SeekBar)findViewById(R.id.deadlineHardnessPicker);
        taskTypeMessage = (TextView)findViewById(R.id.task_type_message);
        taskTypeSpinner = (Spinner)findViewById(R.id.task_type_spinner);

        schoolView = (RelativeLayout)findViewById(R.id.SchoolInfo);
        schoolViewParams = schoolView.getLayoutParams();

        deadlineView = (RelativeLayout)findViewById(R.id.DeadlineInfo);
        deadlineViewParams = deadlineView.getLayoutParams();

        courseSpinner = (Spinner)findViewById(R.id.course_spinner);
        quarterSpinner = (Spinner)findViewById(R.id.quarter_spinner);
        syllabusCategorySpinner = (Spinner)findViewById(R.id.category_spinner);

    }
}
