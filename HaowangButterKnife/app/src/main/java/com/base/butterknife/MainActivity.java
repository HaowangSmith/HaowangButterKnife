package com.base.butterknife;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.base.inject.InjectView;
import com.base.inject_annotion.BindView;


public class MainActivity extends AppCompatActivity {
    @BindView(R.id.text)
    TextView textview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InjectView.bind(this);
        Toast.makeText(this,"--->  "+textview, Toast.LENGTH_SHORT).show();
        textview.setText("6666666");
    }
}
