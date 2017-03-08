package com.yarolegovich.discretescrollview.sample.weather;

/**
 * Created by yarolegovich on 08.03.2017.
 */

public class Forecast {

    private final String cityName;
    private final int cityIcon;
    private final String temperature;
    private final Weather weather;

    public Forecast(String cityName, int cityIcon, String temperature, Weather weather) {
        this.cityName = cityName;
        this.cityIcon = cityIcon;
        this.temperature = temperature;
        this.weather = weather;
    }

    public String getCityName() {
        return cityName;
    }

    public int getCityIcon() {
        return cityIcon;
    }

    public String getTemperature() {
        return temperature;
    }

    public Weather getWeather() {
        return weather;
    }
}
