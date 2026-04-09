// ABOUTME: Activity implementation for the agent's tools.
// Makes HTTP calls to external APIs for IP lookup, geolocation, geocoding, and weather.

package io.temporal.ai.workshop.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component("toolActivitiesImpl")
public class ToolActivitiesImpl implements ToolActivities {

    private static final Logger log = LoggerFactory.getLogger(ToolActivitiesImpl.class);

    @Override
    public String getIpAddress() {
        log.info("Looking up current IP address");
        String ip = HttpHelper.get("https://icanhazip.com").trim();
        log.info("IP address: {}", ip);
        return ip;
    }

    @Override
    public String getLocationInfo(String ipAddress) {
        log.info("Looking up location for IP: {}", ipAddress);
        String response = HttpHelper.get("http://ip-api.com/json/" + ipAddress);
        log.info("Location response: {}", response);
        return response;
    }

    @Override
    public String getCoordinates(String city) {
        log.info("Looking up coordinates for city: {}", city);
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String response = HttpHelper.get("https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1");
        log.info("Geocoding response: {}", response);
        return response;
    }

    @Override
    public String getWeather(double latitude, double longitude) {
        log.info("Looking up weather for lat={}, lon={}", latitude, longitude);
        String url = String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f"
                        + "&current=temperature_2m,weather_code,wind_speed_10m&temperature_unit=fahrenheit",
                latitude, longitude);
        String response = HttpHelper.get(url);
        log.info("Weather response: {}", response);
        return response;
    }
}
