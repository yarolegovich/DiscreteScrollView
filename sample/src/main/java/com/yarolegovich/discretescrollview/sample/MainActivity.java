package com.yarolegovich.discretescrollview.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.sample.shop.ShopActivity;
import com.yarolegovich.discretescrollview.sample.weather.WeatherActivity;
import com.yarolegovich.discretescrollview.transform.Pivot;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final Uri URL_TAYA_BEHANCE = Uri.parse("https://www.behance.net/yurkivt");
    private static final Uri URL_SHOP_PHOTOS = Uri.parse("https://herriottgrace.com/collections/all");
    private static final Uri URL_CITY_ICONS = Uri.parse("https://www.flaticon.com");
    private static final Uri URL_APP_REPO = Uri.parse("https://github.com/yarolegovich/DiscreteScrollView");

    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.screen);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.preview_shop).setOnClickListener(this);
        findViewById(R.id.preview_weather).setOnClickListener(this);

        findViewById(R.id.credit_city_icons).setOnClickListener(this);
        findViewById(R.id.credit_shop_photos).setOnClickListener(this);
        findViewById(R.id.credit_taya).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_github:
                open(URL_APP_REPO);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.preview_shop:
                start(ShopActivity.class);
                break;
            case R.id.preview_weather:
                start(WeatherActivity.class);
                break;
            case R.id.credit_city_icons:
                open(URL_CITY_ICONS);
                break;
            case R.id.credit_shop_photos:
                open(URL_SHOP_PHOTOS);
                break;
            case R.id.credit_taya:
                open(URL_TAYA_BEHANCE);
                break;
        }
    }

    private void open(Uri url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(url);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Snackbar.make(root,
                    R.string.msg_no_browser,
                    Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void start(Class<? extends Activity> token) {
        Intent intent = new Intent(this, token);
        startActivity(intent);
    }
}
