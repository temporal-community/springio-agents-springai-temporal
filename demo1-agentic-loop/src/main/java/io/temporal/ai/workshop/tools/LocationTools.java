// ABOUTME: Tool implementations for IP address lookup, geolocation, and city geocoding.
// The @Tool annotations generate LLM schemas; the methods are the actual implementations.

package io.temporal.ai.workshop.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LocationTools {

    @Tool(description = "Get the IP address of the current machine.")
    public String getIpAddress() {
        return HttpHelper.get("https://icanhazip.com").trim();
    }

    @Tool(description = "Get the location information for an IP address. This includes the city, state, country, latitude, and longitude.")
    public String getLocationInfo(
            @ToolParam(description = "An IP address") String ipAddress) {
        return HttpHelper.get("http://ip-api.com/json/" + ipAddress);
    }

    @Tool(description = "Get the latitude and longitude for a city name.")
    public String getCoordinates(
            @ToolParam(description = "The city name to look up") String city) {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        return HttpHelper.get("https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1");
    }
}
