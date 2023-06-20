package com.google.common.geometry;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class S2PolylineSimplifierTest extends GeometryTestCase {

    private void checkSimplify(String src, String dst, String target, String avoid,
                               List<Boolean> disc_on_left, double radiusDegrees, boolean expectedResult) {
        S1ChordAngle radius = S1ChordAngle.fromS1Angle(S1Angle.degrees(radiusDegrees));
        S2PolylineSimplifier s = new S2PolylineSimplifier();
        s.init(makePoint(src));
        List<S2Point> targets = new ArrayList<>();
        parseVertices(target, targets);
        for (S2Point p : targets) {
            s.targetDisc(p, radius);
        }
        int i = 0;
        List<S2Point> avoids = new ArrayList<>();
        parseVertices(avoid, avoids);
        for (S2Point p : avoids) {
            s.avoidDisc(p, radius, disc_on_left.get(i++));
        }
        assertEquals(expectedResult, s.extend(makePoint(dst)));
    }

    public void testReuse() {
        // Check that Init() can be called more than once.
        S2PolylineSimplifier s = new S2PolylineSimplifier();
        S1ChordAngle radius = S1ChordAngle.fromS1Angle(S1Angle.degrees(10));
        s.init(new S2Point(1, 0, 0));
        assertTrue(s.targetDisc(new S2Point(1, 1, 0).normalize(), radius));
        assertTrue(s.targetDisc(new S2Point(1.0, 1.0, 0.1).normalize(), radius));
        assertFalse(s.extend(new S2Point(1.0, 1.0, 0.4).normalize()));

        s.init(new S2Point(0, 1, 0));
        assertTrue(s.targetDisc(new S2Point(1.0, 1.0, 0.3).normalize(), radius));
        assertTrue(s.targetDisc(new S2Point(1.0, 1.0, 0.2).normalize(), radius));
        assertFalse(s.extend(new S2Point(1.0, 1.0, 0.0).normalize()));
    }

    public void testNoConstraints() {
        // No constraints, dst == src.
        checkSimplify("0:1", "0:1", "", "", new ArrayList<>(), 0.0, true);

        // No constraints, dst != src.
        checkSimplify("0:1", "1:0", "", "", new ArrayList<>(), 0.0, true);

        // No constraints, (src, dst) longer than 90 degrees (not supported).
        checkSimplify("0:0", "0:91", "", "", new ArrayList<>(), 0.0, false);
    }

    public void testTargetOnePoint() {
        // Three points on a straight line.  In theory zero tolerance should work,
        // but in practice there are floating point errors.
        checkSimplify("0:0", "0:2", "0:1", "", new ArrayList<>(), 1e-10, true);

        // Three points where the middle point is too far away.
        checkSimplify("0:0", "0:2", "1:1", "", new ArrayList<>(), 0.9, false);

        // A target disc that contains the source vertex.
        checkSimplify("0:0", "0:2", "0:0.1", "", new ArrayList<>(), 1.0, true);

        // A target disc that contains the destination vertex.
        checkSimplify("0:0", "0:2", "0:2.1", "", new ArrayList<>(), 1.0, true);
    }

    public void testAvoidOnePoint() {
        // Three points on a straight line, attempting to avoid the middle point.
        checkSimplify("0:0", "0:2", "", "0:1", Lists.newArrayList(true), 1e-10, false);
        // Three points where the middle point can be successfully avoided.
        checkSimplify("0:0", "0:2", "", "1:1", Lists.newArrayList(true), 0.9, true);
        // Three points where the middle point is on the left, but where the client
        // requires the point to be on the right of the edge.
        checkSimplify("0:0", "0:2", "", "1:1", Lists.newArrayList(false), 1e-10, false);
    }
    /*
yDir=(0.0, -1.0, 0.0)
xDir=(-0.0, -0.0, 1.0)
getAngle: p=(0.9996954135095479, 0.017449748351250485, 0.01745240643728351), xDir=(-0.0, -0.0, 1.0), yDir=(0.0, -1.0, 0.0)
center=-0.7853220051761581
opposite=2.356270648413635
target=[2.356270648413635, -0.7853220051761581]
window=[3.046158530131703, -1.4752098868942265]
     */

    public void testTargetAndAvoid() {
        // Target several points that are separated from the proposed edge by about
        // 0.7 degrees, and avoid several points that are separated from the
        // proposed edge by about 1.4 degrees.
        checkSimplify(
                "0:0", "10:10", "2:3, 4:3, 7:8",
                "4:2, 7:5, 7:9", Lists.newArrayList(true, true, false), 1.0, true
        );

        // The same example, but one point to be targeted is 1.4 degrees away.
        checkSimplify(
                "0:0", "10:10", "2:3, 4:6, 7:8",
                "4:2, 7:5, 7:9", Lists.newArrayList(true, true, false), 1.0, false
        );

        // The same example, but one point to be avoided is 0.7 degrees away.
        checkSimplify(
                "0:0", "10:10", "2:3, 4:3, 7:8",
                "4:2, 6:5, 7:9", Lists.newArrayList(true, true, false), 1.0, false
        );
    }

    public void testPrecision() {
        // This is a rough upper bound on both the error in constructing the disc
        // locations (i.e., S2::InterpolateAtDistance, etc), and also on the
        // padding that S2PolylineSimplifier uses to ensure that its results are
        // conservative (i.e., the error calculated by GetSemiwidth).
        S1Angle kMaxError = S1Angle.radians(25 * S2.DBL_EPSILON);

        // We repeatedly generate a random edge.  We then target several discs that
        // barely overlap the edge, and avoid several discs that barely miss the
        // edge.  About half the time, we choose one disc and make it slightly too
        // large or too small so that targeting fails.
        int kIters = 1000;  // Passes with 1 million iterations.
        S2PolylineSimplifier simplifier = new S2PolylineSimplifier();

        for (int iter = 0; iter < kIters; iter++) {
            rand.setSeed(iter + 1);  // Easier to reproduce a specific case.
            S2Point src = randomPoint();
            simplifier.init(src);
            S2Point dst = S2EdgeUtil.interpolateAtDistance(
                    S1Angle.radians(rand.nextDouble()),
                    src,
                    randomPoint()
            );
            S2Point n = S2.robustCrossProd(src, dst).normalize();

            // If bad_disc >= 0, then we make targeting fail for that disc.
            int kNumDiscs = 5;
            int bad_disc = rand.nextInt(2 * kNumDiscs) - kNumDiscs;
            for (int i = 0; i < kNumDiscs; i++) {
                double f = rand.nextDouble();
                S2Point a = ((src.mul(1 - f)).add(dst.mul(f))).normalize();
                S1Angle r = S1Angle.radians(rand.nextDouble());
                boolean on_left = oneIn(2);
                S2Point x = S2EdgeUtil.interpolateAtDistance(r, a, on_left ? n : n.neg());
                // We grow the radius slightly if we want to target the disc and shrink
                // it otherwise, *unless* we want targeting to fail for this disc, in
                // which case these actions are reversed.
                boolean avoid = oneIn(2);
                boolean grow_radius = (avoid == (i == bad_disc));
                S1ChordAngle radius = S1ChordAngle.fromS1Angle(grow_radius ? (r.add(kMaxError)) : (r.sub(kMaxError)));
                if (avoid) {
                    simplifier.avoidDisc(x, radius, on_left);
                } else {
                    simplifier.targetDisc(x, radius);
                }
            }
            // The result is true iff all the disc constraints were satisfiable.
            assertEquals(bad_disc < 0, simplifier.extend(dst));
        }
    }

}
