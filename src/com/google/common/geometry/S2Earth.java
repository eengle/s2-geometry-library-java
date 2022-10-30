package com.google.common.geometry;

import static com.google.common.geometry.S2.M_PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.sin;

/**
 * The earth modeled as a sphere.
 */
public class S2Earth {

    /**
     * The Earth's mean radius in meters, which is the radius of the equivalent sphere with the same surface area.
     * According to NASA, this value is 6371.01 +/- 0.02 km.  The equatorial radius is 6378.136 km, and the polar
     * radius is 6356.752 km.  They differ by one part in 298.257.
     * <p>
     * Reference: http://ssd.jpl.nasa.gov/phys_props_earth.html, which quotes
     * Yoder, C.F. 1995. "Astrometric and Geodetic Properties of Earth and the
     * Solar System" in Global Earth Physics, A Handbook of Physical Constants,
     * AGU Reference Shelf 1, American Geophysical Union, Table 2.
     */
    public static double RADIUS_METERS = 6371010.0;

    /**
     * The Earth's mean radius in kilometers.
     */
    public static double RADIUS_KM = 0.001 * RADIUS_METERS;

    /**
     * The altitude of the lowest known point on Earth. The lowest known point on Earth is the Challenger Deep with
     * an altitude of -10898 meters above the surface of the spherical earth.
     */
    public static double LOWEST_ALTITUDE_METERS = -10898;
    /**
     * The altitude of the lowest known point on Earth in kilometers.
     */
    public static double LOWEST_ALTITUDE_KM = 0.001 * LOWEST_ALTITUDE_METERS;

    /**
     * The altitude of the highest known point on Earth. The highest known point on Earth is Mount Everest with an
     * altitude of 8846 meters above the surface of the spherical earth.
     */
    public static double HIGHEST_ALTITUDE_METERS = 8846;
    /**
     * The altitude of the highest known point on Earth in kilometers.
     */
    public static double HIGHEST_ALTITUDE_KM = 0.001 * HIGHEST_ALTITUDE_METERS;

    // These functions convert between distances on the unit sphere
    // (expressed as angles subtended from the sphere's center) and
    // distances on the Earth's surface.  This is possible only because
    // the Earth is modeled as a sphere; otherwise a given angle would
    // correspond to a range of distances depending on where the
    // corresponding line segment was located.

    /**
     * Converts a distance on the Earth's surface given in meters to a distance on the unit sphere.
     *
     * @param meters A distance on the Earth's surface.
     * @return The corresponding distance on the unit sphere.
     */
    public static S1Angle metersToAngle(double meters) {
        return S1Angle.radians(metersToRadians(meters));
    }

    /**
     * Converts a distance on the Earth's surface given in meters to a S1ChordAngle on the unit sphere.
     *
     * @param meters A distance on the Earth's surface.
     * @return The corresponding S1ChordAngle on the unit sphere.
     */
    public static S1ChordAngle metersToChordAngle(double meters) {
        return S1ChordAngle.fromS1Angle(metersToAngle(meters));
    }

    public static double metersToRadians(double meters) {
        return meters / RADIUS_METERS;
    }

    /**
     * Converts a distance on the unit sphere to a distance on the Earth's surface given in meters.
     *
     * @param angle A distance on the unit sphere.
     * @return The corresponding distance on the Earth's surface.
     */
    public static double toMeters(S1Angle angle) {
        return angle.radians() * RADIUS_METERS;
    }

    /**
     * Converts a S1ChordAngle on the unit sphere to a distance on the Earth's surface given in meters.
     *
     * @param cangle A S1ChordAngle on the unit sphere.
     * @return The corresponding distance on the Earth's surface.
     */
    public static double toMeters(S1ChordAngle cangle) {
        return toMeters(cangle.toAngle());
    }

    public static double radiansToMeters(double radians) {
        return radians * RADIUS_METERS;
    }

    /**
     * Converts a distance on the Earth's surface given in kilometers to a distance on the unit sphere.
     *
     * @param km A distance on the Earth's surface.
     * @return The corresponding distance on the unit sphere.
     */
    public static S1Angle kmToAngle(double km) {
        return S1Angle.radians(kmToRadians(km));
    }

    /**
     * Converts a distance on the Earth's surface given in kilometers to a S1ChordAngle on the unit sphere.
     *
     * @param km A distance on the Earth's surface.
     * @return The corresponding S1ChordAngle on the unit sphere.
     */
    public static S1ChordAngle kmToChordAngle(double km) {
        return S1ChordAngle.fromS1Angle(kmToAngle(km));
    }

    public static double kmToRadians(double km) {
        return km / RADIUS_KM;
    }

    /**
     * Converts a distance on the unit sphere to a distance on the Earth's surface given in kilometers.
     *
     * @param angle A distance on the unit sphere.
     * @return The corresponding distance on the Earth's surface.
     */
    public static double toKm(S1Angle angle) {
        return angle.radians() * RADIUS_KM;
    }

    /**
     * Converts a S1ChordAngle on the unit sphere to a distance on the Earth's surface given in kilometers.
     *
     * @param cangle A S1ChordAngle on the unit sphere.
     * @return The corresponding distance on the Earth's surface.
     */
    public static double toKm(S1ChordAngle cangle) {
        return toKm(cangle.toAngle());
    }

    public static double radiansToKm(double radians) {
        return radians * RADIUS_KM;
    }

    // These functions convert between areas on the unit sphere
    // (as returned by the S2 library) and areas on the Earth's surface.
    // Note that the area of a region on the unit sphere is equal to the
    // solid angle it subtends from the sphere's center (measured in steradians).

    /**
     * Converts an area on the Earth's surface given in square kilometers to a steradians area on the unit sphere.
     *
     * @param km2 Area on the Earth's surface.
     * @return The corresponding area on the unit sphere.
     */
    public static double squareKmToSteradians(double km2) {
        return km2 / (RADIUS_KM * RADIUS_KM);
    }

    /**
     * Converts an area on the Earth's surface given in square meters to a steradians area on the unit sphere.
     *
     * @param m2 Area on the Earth's surface.
     * @return The corresponding area on the unit sphere.
     */
    public static double squareMetersToSteradians(double m2) {
        return m2 / (RADIUS_METERS * RADIUS_METERS);
    }

    /**
     * Converts an area on the unit sphere to the Earth's surface given in square kilometers.
     *
     * @param steradians Area on the unit sphere.
     * @return The corresponding area on the Earth's surface.
     */
    public static double steradiansToSquareKm(double steradians) {
        return steradians * RADIUS_KM * RADIUS_KM;
    }

    /**
     * Converts an area on the unit sphere to the Earth's surface given in square meters.
     *
     * @param steradians Area on the unit sphere.
     * @return The corresponding area on the Earth's surface.
     */
    public static double steradiansToSquareMeters(double steradians) {
        return steradians * RADIUS_METERS * RADIUS_METERS;
    }

    // Convenience functions for the frequent case where you need to call
    // toRadians in order to convert an east-west distance on the globe to
    // radians. The output is a function of how close to the poles you are
    // (i.e. at the bulge at the equator, one unit of longitude represents a
    // much farther distance). The function will never return more than 2*PI
    // radians, even if you're trying to go 100 million miles west at the north
    // pole.

    public static double metersToLongitudeRadians(double meters, double latitudeRadians) {
        double scalar = cos(latitudeRadians);
        if (scalar == 0) return M_PI * 2;
        return min(metersToRadians(meters) / scalar, M_PI * 2);
    }

    public static double kmToLongitudeRadians(double km, double latitude_radians) {
        return metersToLongitudeRadians(1000 * km, latitude_radians);
    }

    /**
     * Computes the initial bearing from a to b. This is the bearing an observer at point a has when facing point b.
     * A bearing of 0 degrees is north, and it increases clockwise (90 degrees is east, etc).
     *
     * If a == b, a == -b, or a is one of the Earths' poles, the return value is undefined.
     * Sourced from http://www.movable-type.co.uk/scripts/latlong.html.
     *
     * @param a A lat-lng point
     * @param b A lat-lng point
     * @return The initial bearing from a to b.
     */
    public static S1Angle getInitialBearing(S2LatLng a, S2LatLng b) {
        double lat1 = a.lat().radians();
        double cosLat2 = cos(b.lat().radians());
        double lat_diff = b.lat().radians() - a.lat().radians();
        double lng_diff = b.lng().radians() - a.lng().radians();

        double x = sin(lat_diff) + sin(lat1) * cosLat2 * 2 * haversine(lng_diff);
        double y = sin(lng_diff) * cosLat2;
        return S1Angle.radians(atan2(y, x));
    }

    /**
     * Computes the distance in meters between 2 points on the Earth's surface given by their coordinates on
     * the unit sphere.
     *
     * @param a A S2Point.
     * @param b A S2Point.
     * @return The distance in meters.
     */
    public static double getDistanceMeters(S2Point a, S2Point b) {
        return radiansToMeters(a.angle(b));
    }

    /**
     * Computes the distance in meters between 2 points on the Earth's surface.
     *
     * @param a A lat;lng point.
     * @param b A lat;lng point.
     * @return The distance in meters.
     */
    public static double getDistanceMeters(S2LatLng a, S2LatLng b) {
        return toMeters(a.getDistance(b));
    }

    /**
     * Computes the distance in kilometers between 2 points on the Earth's surface given by their coordinates on
     * the unit sphere.
     *
     * @param a A S2Point.
     * @param b A S2Point.
     * @return The distance in kilometers.
     */
    public static double getDistanceKm(S2Point a, S2Point b) {
        return radiansToKm(a.angle(b));
    }

    /**
     * Computes the distance in kilometers between 2 points on the Earth's surface.
     *
     * @param a A lat;lng point.
     * @param b A lat;lng point.
     * @return The distance in kilometers.
     */
    public static double getDistanceKm(S2LatLng a, S2LatLng b) {
        return toKm(a.getDistance(b));
    }

    /**
     * Computes the haversine function of an angle given in radian.
     * <p>
     * http://en.wikipedia.org/wiki/Haversine_formula
     * haversine(x) has very good numerical stability around zero.
     * haversine(x) == (1-cos(x))/2 == sin(x/2)^2; must be implemented with the second form to reap the numerical
     * benefits.
     *
     * @param radians An angle.
     * @return The haversine function result.
     */
    public static double haversine(double radians) {
        double sinHalf = sin(radians / 2);
        return sinHalf * sinHalf;
    }

}
