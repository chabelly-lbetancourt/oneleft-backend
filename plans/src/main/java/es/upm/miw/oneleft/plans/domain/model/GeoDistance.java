package es.upm.miw.oneleft.plans.domain.model;

/**
 * Distance over the Earth's surface with the haversine formula. The database filters with PostGIS; this function is
 * used to show the distance and to decide in memory whether a newly published plan is relevant to someone watching
 * the list.
 */
public final class GeoDistance {

    /** Mean Earth radius (IUGG), in metres. */
    static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private GeoDistance() {
    }

    public static double meters(double latitude1, double longitude1, double latitude2, double longitude2) {
        var phi1 = Math.toRadians(latitude1);
        var phi2 = Math.toRadians(latitude2);
        var deltaPhi = Math.toRadians(latitude2 - latitude1);
        var deltaLambda = Math.toRadians(longitude2 - longitude1);
        var a = Math.pow(Math.sin(deltaPhi / 2), 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(deltaLambda / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
