package com.crash.view;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.crash.utils.R;

/**
 * @Author tgl
 * @Date 2022/5/5 下午3:06
 * @Version 1.0
 * @Description TODO
 */
public class ShowDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_detail);
        TextView tv = findViewById(R.id.tv_detail);
        tv.setText(getIntent().getStringExtra("data"));
    }
}
