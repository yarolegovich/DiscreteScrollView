package com.yarolegovich.discretescrollview.sample.weather;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.sample.R;
import com.yarolegovich.discretescrollview.sample.DiscreteScrollViewOptions;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import java.util.List;

/**
 * Created by yarolegovich on 08.03.2017.
 */

public class WeatherActivity extends AppCompatActivity implements
        DiscreteScrollView.ScrollStateChangeListener<ForecastAdapter.ViewHolder>,
        DiscreteScrollView.CurrentItemChangeListener<ForecastAdapter.ViewHolder>,
        View.OnClickListener {

    private List<Forecast> forecasts;

    private ForecastView forecastView;
    private DiscreteScrollView cityPicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        forecastView = (ForecastView) findViewById(R.id.forecast_view);

        forecasts = WeatherStation.get().getForecasts();
        cityPicker = (DiscreteScrollView) findViewById(R.id.forecast_city_picker);
        cityPicker.setAdapter(new ForecastAdapter(forecasts));
        cityPicker.setCurrentItemChangeListener(this);
        cityPicker.setScrollStateChangeListener(this);
        cityPicker.scrollToPosition(2);
        cityPicker.setItemTransitionTimeMillis(DiscreteScrollViewOptions.getTransitionTime());
        cityPicker.setItemTransformer(new ScaleTransformer.Builder()
                .setMinScale(0.8f)
                .build());

        forecastView.setForecast(forecasts.get(0));

        findViewById(R.id.home).setOnClickListener(this);
        findViewById(R.id.btn_transition_time).setOnClickListener(this);
        findViewById(R.id.btn_smooth_scroll).setOnClickListener(this);
    }

    @Override
    public void onCurrentItemChanged(ForecastAdapter.ViewHolder holder, int position) {
        forecastView.setForecast(forecasts.get(position));
        holder.showText();
    }

    @Override
    public void onScrollStart(ForecastAdapter.ViewHolder holder, int position) {
        holder.hideText();
    }

    @Override
    public void onScroll(float position) {
        Forecast current = forecasts.get(cityPicker.getCurrentItem());
        int nextPosition = cityPicker.getCurrentItem() + (position > 0 ? -1 : 1);
        if (nextPosition >= 0 && nextPosition < cityPicker.getAdapter().getItemCount()) {
            Forecast next = forecasts.get(nextPosition);
            forecastView.onScroll(1f - Math.abs(position), current, next);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:
                finish();
                break;
            case R.id.btn_transition_time:
                DiscreteScrollViewOptions.configureTransitionTime(cityPicker);
                break;
            case R.id.btn_smooth_scroll:
                DiscreteScrollViewOptions.smoothScrollToUserSelectedPosition(cityPicker, v);
                break;
        }
    }

    @Override
    public void onScrollEnd(ForecastAdapter.ViewHolder holder, int position) {

    }
}
