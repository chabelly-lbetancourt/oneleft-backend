package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.DeviceLocation;

import java.util.UUID;

/** Output port: whether a location is within some metres of the meeting point of a plan (HU-025). */
public interface MeetingPointGeofence {

    boolean contains(UUID planId, DeviceLocation location, int meters);
}
