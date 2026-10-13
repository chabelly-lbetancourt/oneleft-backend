package es.upm.miw.oneleft.plans.domain.model;

import java.io.Serializable;

/**
 * A place found by a geocoder for the meeting point (HU-071): its name, a readable address and where it is.
 *
 * @param address street, neighbourhood and city, as far as they are known; empty if none
 */
public record Place(String name, String address, double latitude, double longitude) implements Serializable {
}
