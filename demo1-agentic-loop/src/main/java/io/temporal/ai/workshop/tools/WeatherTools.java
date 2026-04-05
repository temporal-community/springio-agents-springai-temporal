// ABOUTME: Tool implementation for weather lookup via Open-Meteo.
// The @Tool annotation generates the LLM schema; the method is the actual implementation.

package io.temporal.ai.workshop.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class WeatherTools {

    @Tool(description = "Get current weather for a location using latitude and longitude. Returns temperature in Fahrenheit, weather code, and wind speed.")
    public String getWeather(
            @ToolParam(description = "Latitude of the location") double latitude,
            @ToolParam(description = "Longitude of the location") double longitude) {
        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f"
                        + "&current=temperature_2m,weather_code,wind_speed_10m&temperature_unit=fahrenheit",
                latitude, longitude);
        return HttpHelper.get(url);
    }
}
