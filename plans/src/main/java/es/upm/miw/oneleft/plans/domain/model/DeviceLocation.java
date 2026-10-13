package es.upm.miw.oneleft.plans.domain.model;

/**
 * Where the device is when someone checks in (HU-025). It is only compared with the meeting point and never kept.
 */
public record DeviceLocation(double latitude, double longitude) {

    public DeviceLocation {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("coordinates.outOfRange", "Coordinates out of range");
        }
    }
}
