// ABOUTME: Defines tool schemas for the LLM via Spring AI @Tool annotations.
// These methods exist only for schema generation - actual execution happens in ToolActivities.

package io.temporal.ai.workshop.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ToolSchemas {

    @Tool(description = "Get the IP address of the current machine.")
    public String getIpAddress() {
        throw new UnsupportedOperationException("Schema only - use ToolActivities");
    }

    @Tool(description = "Get the location information for an IP address. This includes the city, state, country, latitude, and longitude.")
    public String getLocationInfo(
            @ToolParam(description = "An IP address") String ipAddress) {
        throw new UnsupportedOperationException("Schema only - use ToolActivities");
    }

    @Tool(description = "Get the latitude and longitude for a city name.")
    public String getCoordinates(
            @ToolParam(description = "The city name to look up") String city) {
        throw new UnsupportedOperationException("Schema only - use ToolActivities");
    }

    @Tool(description = "Get current weather for a location using latitude and longitude. Returns temperature in Fahrenheit, weather code, and wind speed.")
    public String getWeather(
            @ToolParam(description = "Latitude of the location") double latitude,
            @ToolParam(description = "Longitude of the location") double longitude) {
        throw new UnsupportedOperationException("Schema only - use ToolActivities");
    }
}
