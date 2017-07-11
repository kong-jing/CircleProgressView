package com.jing.circleprogressview;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
  ProgressView pv;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initData();
    pv.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        double score = Double.parseDouble(format2(Math.random()*100));
        pv.setScore(score );
        pv.setCurrentCount((float) (score * 2/3));
      }
    });
  }

  private void initData() {
    pv = (ProgressView) findViewById(R.id.progress);
    pv.setMaxCount(0.66f);
    pv.setCurrentCount(0.0f);
    pv.setScore(0.0);
    pv.setBackgroundColor(new int[] { Color.rgb(54, 176, 234), Color.rgb(54, 176, 234), Color.rgb(54, 176, 234) });
  }

  public static String format2(double value) {

    DecimalFormat df = new DecimalFormat("0.00");
    df.setRoundingMode(RoundingMode.HALF_UP);
    return df.format(value);
  }

}
