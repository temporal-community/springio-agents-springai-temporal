// ABOUTME: Activity interface for the agent's tools, with dual @ActivityMethod + @Tool annotations.
// Serves as both the Temporal activity contract and the Spring AI tool schema definition.

package io.temporal.ai.workshop.tools;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@ActivityInterface
public interface ToolActivities {

    @ActivityMethod
    @Tool(description = "Get the IP address of the current machine.")
    String getIpAddress();

    @ActivityMethod
    @Tool(description = "Get the location information for an IP address. This includes the city, state, country, latitude, and longitude.")
    String getLocationInfo(@ToolParam(description = "An IP address") String ipAddress);

    @ActivityMethod
    @Tool(description = "Get the latitude and longitude for a city name.")
    String getCoordinates(@ToolParam(description = "The city name to look up") String city);

    @ActivityMethod
    @Tool(description = "Get current weather for a location using latitude and longitude. Returns temperature in Fahrenheit, weather code, and wind speed.")
    String getWeather(
            @ToolParam(description = "Latitude of the location") double latitude,
            @ToolParam(description = "Longitude of the location") double longitude);
}
