package com.google.common.geometry;

import static com.google.common.geometry.S2.M_PI;
import static com.google.common.geometry.S2.M_PI_2;
import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class S2EarthTest extends GeometryTestCase {

    public void testAngleConversion() {
        assertDoubleNear(S2Earth.metersToAngle(S2Earth.RADIUS_METERS).radians(), 1.0, 1e-15);
        assertDoubleNear(S2Earth.metersToChordAngle(S2Earth.RADIUS_METERS).toAngle().radians(), 1.0, 1e-15);
        assertDoubleNear(S2Earth.toMeters(S1Angle.radians(2)), 2 * S2Earth.RADIUS_METERS, 1e-7);
        assertDoubleNear(S2Earth.toMeters(S1ChordAngle.fromS1Angle(S1Angle.radians(2.0))), 2 * S2Earth.RADIUS_METERS, 1e-7);
        assertDoubleNear(S2Earth.metersToRadians(S2Earth.RADIUS_METERS), 1.0, 1e-15);
        assertDoubleNear(S2Earth.toMeters(S1Angle.degrees(180)), S2Earth.RADIUS_METERS * M_PI, 1e-8);
        assertDoubleNear(S2Earth.toMeters(S1ChordAngle.fromS1Angle(S1Angle.degrees(180))), S2Earth.RADIUS_METERS * M_PI, 1e-8);
        assertDoubleNear(S2Earth.radiansToKm(0.5), 0.5 * S2Earth.RADIUS_KM, 1e-8);
        assertDoubleNear(S2Earth.toKm(S1ChordAngle.fromS1Angle(S1Angle.radians(0.5))), 0.5 * S2Earth.RADIUS_KM, 1e-8);
        assertDoubleNear(S2Earth.kmToRadians(S2Earth.RADIUS_METERS / 1000), 1.0, 1e-15);
        assertDoubleNear(S2Earth.radiansToKm(0.5), 0.5 * S2Earth.RADIUS_KM, 1e-8);
        assertDoubleNear(S2Earth.metersToRadians(S2Earth.radiansToKm(0.3) * 1000), 0.3, 1e-15);
        assertDoubleNear(S2Earth.radiansToMeters(S2Earth.kmToRadians(2.5)), 2500.0, 1e-8);
    }

    public void testSolidAngleConversion() {
        assertDoubleNear(S2Earth.squareKmToSteradians(pow((S2Earth.RADIUS_METERS / 1000), 2.0)), 1.0, 1e-15);
        assertDoubleNear(S2Earth.steradiansToSquareKm(pow(0.5, 2.0)), pow((0.5 * S2Earth.RADIUS_KM), 2.0), 1e-8);
        assertDoubleNear(S2Earth.squareMetersToSteradians(pow(S2Earth.radiansToKm(0.3) * 1000, 2.0)), pow(0.3, 2.0), 1e-15);
        assertDoubleNear(S2Earth.steradiansToSquareMeters(pow(S2Earth.kmToRadians(2.5), 2.0)), pow(2500.0, 2.0), 1e-8);
    }

    public void testToLongitudeRadians() {
        // At the equator, ToLongitudeRadians behaves exactly like ToRadians.
        assertDoubleNear(S2Earth.metersToLongitudeRadians(S2Earth.RADIUS_METERS, 0.0), 1.0, 1e-15);

        // The closer we get to the poles, the more radians we need to go the same
        // distance.
        assertTrue(
                S2Earth.metersToLongitudeRadians(S2Earth.RADIUS_METERS, 0.5) >
                        S2Earth.metersToLongitudeRadians(S2Earth.RADIUS_METERS, 0.4)
        );

        // At the poles, we should return 2PI radians instead of dividing by 0.
        assertDoubleNear(S2Earth.metersToLongitudeRadians(S2Earth.RADIUS_METERS, M_PI_2), M_PI * 2, 1e-15);

        // Within epsilon of the poles, we should still return 2PI radians instead
        // of directing the caller to take thousands of radians around.
        assertDoubleNear(S2Earth.metersToLongitudeRadians(S2Earth.RADIUS_METERS, M_PI_2 - 1e-4), M_PI * 2, 1e-15);
    }

    public void testGetInitialBearing() {
        for (TestConfig config : TestConfig.values()) {
            getInitialBearing(config);
        }
    }

    public void getInitialBearing(TestConfig config) {
        S1Angle bearing = S2Earth.getInitialBearing(config.a, config.b);
        double angleDiff = abs((bearing.sub(config.bearing)).normalize().degrees());
        assertTrue(angleDiff <= 1e-2);
    }

    public void testGetDistance() {
        S2Point north = new S2Point(0, 0, 1);
        S2Point south = new S2Point(0, 0, -1);
        S2Point west = new S2Point(0, -1, 0);

        assertDoubleNear(S2Earth.getDistanceMeters(north, south), M_PI * S2Earth.RADIUS_METERS, 1e-7);
        assertDoubleNear(S2Earth.getDistanceMeters(west, west), 0.0, 1e-15);
        assertDoubleNear(S2Earth.getDistanceMeters(north, west), M_PI_2 * S2Earth.RADIUS_METERS, 1e-8);

        assertDoubleNear(
                S2Earth.getDistanceMeters(S2LatLng.fromDegrees(0, -90), S2LatLng.fromDegrees(-90, -38)),
                S2Earth.getDistanceMeters(west, south),
                1e-7
        );

        assertDoubleNear(
                S2Earth.getDistanceKm(S2LatLng.fromRadians(0.0, 0.6), S2LatLng.fromRadians(0.0, -0.4)),
                S2Earth.RADIUS_KM,
                1e-8
        );

        assertDoubleNear(
                S2Earth.getDistanceMeters(S2LatLng.fromDegrees(80, 27), S2LatLng.fromDegrees(55, -153)),
                1000 * S2Earth.RADIUS_KM * M_PI / 4,
                1e-8
        );
    }

    enum TestConfig {
        WESTWARD_IN_EQUATOR(
                "Westward on equator",
                S2LatLng.fromDegrees(0, 50),
                S2LatLng.fromDegrees(0, 100),
                S1Angle.degrees(90)
        ),
        EASTWARD_ON_EQUATOR(
                "Eastward on equator",
                S2LatLng.fromDegrees(0, 50),
                S2LatLng.fromDegrees(0, 0),
                S1Angle.degrees(-90)
        ),
        NORTHWARD_ON_MERIDIAN(
                "Northward on meridian",
                S2LatLng.fromDegrees(16, 28),
                S2LatLng.fromDegrees(81, 28),
                S1Angle.degrees(0)
        ),
        SOUTHWARD_ON_MERIDIAN(
                "Southward on meridian",
                S2LatLng.fromDegrees(24, 64),
                S2LatLng.fromDegrees(-27, 64),
                S1Angle.degrees(180)
        ),
        TOWARDS_NORTH_POLE(
                "Towards north pole",
                S2LatLng.fromDegrees(12, 76),
                S2LatLng.fromDegrees(90, 50),
                S1Angle.degrees(0)
        ),
        TOWARDS_SOUTH_POLE(
                "Towards south pole",
                S2LatLng.fromDegrees(-35, 105),
                S2LatLng.fromDegrees(-90, -120),
                S1Angle.degrees(180)
        ),
        SPAIN_TO_JAPAN(
                "Spain to Japan",
                S2LatLng.fromDegrees(40.4379332, -3.749576),
                S2LatLng.fromDegrees(35.6733227, 139.6403486),
                S1Angle.degrees(29.2)
        ),
        JAPAN_TO_SPAIN(
                "Japan to Spain",
                S2LatLng.fromDegrees(35.6733227, 139.6403486),
                S2LatLng.fromDegrees(40.4379332, -3.749576),
                S1Angle.degrees(-27.2)
        );

        String description;
        S2LatLng a;
        S2LatLng b;
        S1Angle bearing;

        TestConfig(String description, S2LatLng a, S2LatLng b, S1Angle bearing) {
            this.description = description;
            this.a = a;
            this.b = b;
            this.bearing = bearing;
        }
    }

}
