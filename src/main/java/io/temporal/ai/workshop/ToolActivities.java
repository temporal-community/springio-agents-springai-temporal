// ABOUTME: Temporal activity interface for the agent's tools.
// Each tool makes external HTTP calls, so they must be activities (not workflow code).

package io.temporal.ai.workshop;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ToolActivities {

    @ActivityMethod
    String getIpAddress();

    @ActivityMethod
    String getLocationInfo(String ipAddress);

    @ActivityMethod
    String getCoordinates(String city);

    @ActivityMethod
    String getWeather(double latitude, double longitude);
}
