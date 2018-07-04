package com.example.sabre.stopwatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class SettingsActivity extends Activity{
    private EditText startDateEditText;
    private EditText endDateEditText;
    private SeekBar percentageOfGradeChooser;
    private TextView percentageLabel;
    private MisterTicker ticker;
    private Spinner quarterForSyllabusSelector;
    private Spinner courseForSyllabusSelector;
    private Spinner quarterForCourseSelector;
    private String TAG = "SOURBEAN";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ticker = new MisterTicker();
        assignButtons();
        MisterTicker.populateQuarterSpinner(quarterForSyllabusSelector, getBaseContext());
        MisterTicker.populateQuarterSpinner(quarterForCourseSelector, getBaseContext());
        quarterForSyllabusSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedQuarterKeyName = quarterForSyllabusSelector.getSelectedItem().toString();
                MisterTicker.populateCourseSpinner(courseForSyllabusSelector, selectedQuarterKeyName, getBaseContext());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void onClickAddNewQuarter(View view) throws ParseException {
        startDateEditText.getText();
        endDateEditText.getText();
        Date endDate;
        Date startDate;
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy");
        java.util.Date myDate;
        startDate = df.parse(startDateEditText.getText().toString().trim());
        endDate = df.parse(endDateEditText.getText().toString().trim());
        Calendar startCalender = Calendar.getInstance();
        Calendar endCalender = Calendar.getInstance();
        startCalender.setTime(startDate);
        endCalender.setTime(endDate);
        MisterTicker.addQuarter(startCalender.getTimeInMillis(), endCalender.getTimeInMillis());

        boolean wasValidChoice = MisterTicker.populateQuarterSpinner(quarterForSyllabusSelector, getBaseContext());
        courseForSyllabusSelector.setEnabled(wasValidChoice);
        MisterTicker.populateQuarterSpinner(quarterForCourseSelector, getBaseContext());

    }

    public void onClickAddNewCourse(View view){
        Spinner quarterSelector = (Spinner)findViewById(R.id.quarter_of_course);
        EditText catalogName = (EditText) findViewById(R.id.catalog_name);
        EditText fullCourseName = (EditText)findViewById(R.id.full_course_name);
        RatingBar credits = (RatingBar)findViewById(R.id.credits);
        SeekBar difficulty = (SeekBar)findViewById(R.id.difficulty);
        int creditNum = credits.getNumStars();
        int difficultyRating = difficulty.getProgress();
        String fullCourseNameString = fullCourseName.getText().toString();
        String catalogNameString = catalogName.getText().toString();
        String quarterString = quarterSelector.getSelectedItem().toString();

        MisterTicker.addCourse(catalogNameString, fullCourseNameString, creditNum, difficultyRating, quarterString);
        MisterTicker.populateCourseSpinner(courseForSyllabusSelector, quarterString, getBaseContext());
    }
    public void onClickGradePercentage(View view){
        percentageLabel.setText(percentageOfGradeChooser.getProgress());

    }
    public void onClickAddNewSyllabusCategory(View view){
        Spinner categoryTypeSelector = (Spinner)findViewById(R.id.syllabus_category_type);
        EditText categoryName = (EditText)findViewById(R.id.syllabus_category_name);
        String categoryType = categoryTypeSelector.getSelectedItem().toString();
        String categoryNameString = categoryName.getText().toString();
        double percentageOfGrade = percentageOfGradeChooser.getProgress();

        String selectedQuarterKeyName = quarterForSyllabusSelector.getSelectedItem().toString();
        String selectedCourseKeyName = courseForSyllabusSelector.getSelectedItem().toString();
        MisterTicker.addSyllabusCategory(categoryNameString, categoryType, percentageOfGrade, selectedQuarterKeyName, selectedCourseKeyName);

    }

    public void onClickDone(View view){
        Intent backIntent = new Intent(this, InfoGatheringActivity.class);
        startActivity(backIntent);
        finish();
    }



    public void assignButtons(){
        startDateEditText = (EditText)findViewById(R.id.start_date);
        endDateEditText = (EditText)findViewById(R.id.end_date);
        percentageOfGradeChooser = (SeekBar)findViewById(R.id.percentage_of_grade);
        percentageLabel = (TextView)findViewById(R.id.percentage_label);
        quarterForSyllabusSelector = (Spinner)findViewById(R.id.quarter_of_category);
        quarterForCourseSelector = (Spinner)findViewById(R.id.quarter_of_course);
        courseForSyllabusSelector = (Spinner)findViewById(R.id.course_of_category);

    }

}
