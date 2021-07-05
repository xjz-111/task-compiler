package com.leslie.compiler;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.leslie.task_annotation.Task;


@Task
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
