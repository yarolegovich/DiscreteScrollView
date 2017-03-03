package com.yarolegovich.discretescrollview.sample;

import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.yarolegovich.discretescrollview.transform.DiscreteScrollItemTransformer;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.transform.Pivot;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private DiscreteScrollView list1, list2;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list1 = (DiscreteScrollView) findViewById(R.id.list_top);
        list1.setAdapter(new DiscreteScrollViewAdapter(R.layout.item_card));
        list1.setItemTransformer(new ScaleTransformer.Builder()
                .setMaxScale(1.05f)
                .setMinScale(0.85f)
                .setPivotY(Pivot.Y.BOTTOM)
                .build());

        list2 = (DiscreteScrollView) findViewById(R.id.list_bot);
        list2.setAdapter(new DiscreteScrollViewAdapter(R.layout.item_small_card));
        list2.setItemTransformer(new ScaleTransformer.Builder()
                .setMaxScale(1.15f)
                .setMinScale(0.85f)
                .build());

        input = (EditText) findViewById(R.id.input);

        findViewById(R.id.btn_scroll).setOnClickListener(this);
        findViewById(R.id.btn_smooth_scroll).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scroll:
                list1.scrollToPosition(getInput());
                list2.scrollToPosition(getInput());
                break;
            case R.id.btn_smooth_scroll:
                list1.smoothScrollToPosition(getInput());
                list2.smoothScrollToPosition(getInput());
                break;
        }

    }

    private int getInput() {
        try {
            return Integer.parseInt(input.getText().toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
