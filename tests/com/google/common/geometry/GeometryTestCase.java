/*
 * Copyright 2005 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.geometry;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.geometry.S2Shape.MutableEdge;
import com.google.common.primitives.UnsignedLongs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;

/** Common code for geometry tests. */
@GwtCompatible(emulated = true)
public strictfp class GeometryTestCase extends TestCase {
  /** Desired ratio between the radii of crossing loops. */
  private static final double DEFAULT_CROSSING_RADIUS_RATIO = 10.0;

  /** The default radius for loops. */
  static final S1Angle DEFAULT_RADIUS = kmToAngle(10.0);

  /**
   * Approximate "effective" radius of the Earth in meters.
   */
  static final double EARTH_RADIUS_METERS = 6371010.0;

  public Random rand;

  @Override
  protected void setUp() {
    rand = new Random(123455);
  }

  public void assertDoubleNear(double a, double b) {
    assertDoubleNear(a, b, 1e-9);
  }

  public void assertDoubleNear(double a, double b, double error) {
    assertTrue("(a + error) = (" + a + " + " + error + ") = " + (a + error) + " <= b=" + b , a + error > b);
    assertTrue(a < b + error);
  }

  /**
   * Checks that the 3D distance between {@code expected} and {@code actual} is at most {@code eps}
   * units.
   */
  public void assertEquals(S2Point expected, S2Point actual, double eps) {
    assertTrue(
        "expected: " + expected + " but was: " + actual, expected.getDistance2(actual) < eps * eps);
  }

  // maybe these should be put in a special testing util class
  /** Return a random unit-length vector. */
  public S2Point randomPoint() {
    return S2Point.normalize(
        new S2Point(
            2 * rand.nextDouble() - 1, 2 * rand.nextDouble() - 1, 2 * rand.nextDouble() - 1));
  }

  /**
   * Return a random cell id at the given level or at a randomly chosen level. The distribution is
   * uniform over the space of cell ids, but only approximately uniform over the surface of the
   * sphere.
   */
  public S2CellId getRandomCellId(int level) {
    int face = random(S2CellId.NUM_FACES);
    long pos = rand.nextLong() & ((1L << (2 * S2CellId.MAX_LEVEL)) - 1);
    return S2CellId.fromFacePosLevel(face, pos, level);
  }

  public S2CellId getRandomCellId() {
    return getRandomCellId(random(S2CellId.MAX_LEVEL + 1));
  }

  /** Return a uniformly distributed "double" in the range [min, max). */
  public double uniform(double min, double max) {
    // assertTrue(min <= max);
    return min + rand.nextDouble() * (max - min);
  }

  /** Return a uniformly distributed integer in the range [0,bound). Requires bound > 0. */
  public int uniform(int bound) {
    Preconditions.checkState(bound > 0);
    return rand.nextInt(bound);
  }

  /** Returns true randomly, approximately one time in {@code n} calls. */
  public boolean oneIn(int n) {
    return rand.nextInt(n) == 0;
  }

  /** Return a right-handed coordinate frame (three orthonormal vectors). */
  public Matrix3x3 getRandomFrame() {
    return getRandomFrameAt(randomPoint());
  }

  /**
   * Given a unit-length z-axis, compute x- and y-axes such that (x,y,z) is a right-handed
   * coordinate frame (three orthonormal vectors).
   */
  public Matrix3x3 getRandomFrameAt(S2Point z) {
    S2Point x = S2Point.normalize(S2Point.crossProd(z, randomPoint()));
    S2Point y = S2Point.normalize(S2Point.crossProd(z, x));
    return Matrix3x3.fromCols(x, y, z);
  }

  int random(int n) {
    if (n == 0) {
      return 0;
    }
    return rand.nextInt(n);
  }

  /**
   * Picks a "base" uniformly from range [0,maxLog] and then return "base" random bits. The effect
   * is to pick a number in the range [0,2^maxLog-1] with bias towards smaller numbers.
   */
  int skewed(int maxLog) {
    final int base = Math.abs(rand.nextInt()) % (maxLog + 1);
    // if (!base) return 0; // if 0==base, we & with 0 below.
    //
    // this distribution differs slightly from ACMRandom's Skewed,
    // since 0 occurs approximately 3 times more than 1 here, and
    // ACMRandom's Skewed never outputs 0.
    return rand.nextInt() & ((1 << base) - 1);
  }

  /**
   * As {@link #checkCovering(S2Region, S2CellUnion, boolean, S2CellId)}, but creates a default and
   * invalid S2CellId for the last argument.
   */
  void checkCovering(S2Region region, S2CellUnion covering, boolean checkTight) {
    checkCovering(region, covering, checkTight, new S2CellId());
  }

  /**
   * Checks that "covering" completely covers the given region. If "checkTight" is true, also checks
   * that it does not contain any cells that do not intersect the given region. ("id" is only used
   * internally.)
   */
  void checkCovering(S2Region region, S2CellUnion covering, boolean checkTight, S2CellId id) {
    if (!id.isValid()) {
      for (int face = 0; face < 6; ++face) {
        checkCovering(region, covering, checkTight, S2CellId.fromFacePosLevel(face, 0, 0));
      }
      return;
    }

    if (!region.mayIntersect(new S2Cell(id))) {
      // If region does not intersect id, then neither should the covering.
      if (checkTight) {
        assertTrue(!covering.intersects(id));
      }

    } else if (!covering.contains(id)) {
      // The region may intersect id, but we can't assert that the covering
      // intersects id because we may discover that the region does not actually
      // intersect upon further subdivision. (MayIntersect is not exact.)
      assertTrue(!region.contains(new S2Cell(id)));
      assertTrue(!id.isLeaf());
      S2CellId end = id.childEnd();
      for (S2CellId child = id.childBegin(); !child.equals(end); child = child.next()) {
        checkCovering(region, covering, checkTight, child);
      }
    }
  }

  S2Cap getRandomCap(double minArea, double maxArea) {
    double capArea = maxArea * Math.pow(minArea / maxArea, rand.nextDouble());
    assertTrue(capArea >= minArea && capArea <= maxArea);

    // The surface area of a cap is 2*Pi times its height.
    return S2Cap.fromAxisArea(randomPoint(), capArea);
  }

  /** Returns a polygon with given center, number of concentric loops, and vertices per loop. */
  static S2Polygon concentricLoopsPolygon(S2Point center, int numLoops, int numVerticesPerLoop) {
    Matrix3x3 m = S2.getFrame(center);
    List<S2Loop> loops = new ArrayList<S2Loop>(numLoops);
    for (int li = 0; li < numLoops; ++li) {
      List<S2Point> vertices = new ArrayList<S2Point>(numVerticesPerLoop);
      double radius = 0.005 * (li + 1) / numLoops;
      double radianStep = 2 * S2.M_PI / numVerticesPerLoop;
      for (int vi = 0; vi < numVerticesPerLoop; ++vi) {
        double angle = vi * radianStep;
        S2Point p = new S2Point(radius * Math.cos(angle), radius * Math.sin(angle), 1);
        vertices.add(S2.rotate(p, m));
      }
      loops.add(new S2Loop(vertices));
    }
    return new S2Polygon(loops);
  }

  S2Point samplePoint(S2Cap cap) {
    // We consider the cap axis to be the "z" axis. We choose two other axes to
    // complete the coordinate frame.

    S2Point z = cap.axis();
    S2Point x = S2.ortho(z);
    S2Point y = S2Point.crossProd(z, x);

    // The surface area of a spherical cap is directly proportional to its
    // height. First we choose a random height, and then we choose a random
    // point along the circle at that height.

    double h = rand.nextDouble() * cap.height();
    double theta = 2 * S2.M_PI * rand.nextDouble();
    double r = Math.sqrt(h * (2 - h)); // Radius of circle.

    // (cos(theta)*r*x + sin(theta)*r*y + (1-h)*z).Normalize()
    return S2Point.normalize(
        S2Point.add(
            S2Point.add(S2Point.mul(x, Math.cos(theta) * r), S2Point.mul(y, Math.sin(theta) * r)),
            S2Point.mul(z, (1 - h))));
  }

  /** Return a random point within the given S2LatLngRect. */
  S2Point samplePoint(S2LatLngRect rect) {
    // First choose a latitude uniformly with respect to area on the sphere.
    double sinLo = Math.sin(rect.lat().lo());
    double sinHi = Math.sin(rect.lat().hi());
    double lat = Math.asin(sinLo + rand.nextDouble() * (sinHi - sinLo));

    // Now choose longitude uniformly within the given range.
    double lng = rect.lng().lo() + rand.nextDouble() * rect.lng().getLength();

    return S2LatLng.fromRadians(lat, lng).normalized().toPoint();
  }

  static S2LatLngRect parseVertices(String str, List<S2Point> vertices) {
    return parseVertices(str, vertices, -1);
  }

  static S2LatLngRect parseVertices(String str, List<S2Point> vertices, int level) {
    if (str == null) {
      return null;
    }

    S2LatLngRect.Builder builder = S2LatLngRect.Builder.empty();
    for (String token : Splitter.on(',').trimResults().omitEmptyStrings().split(str)) {
      int colon = token.indexOf(':');
      if (colon == -1) {
        throw new IllegalArgumentException(
            "Illegal string:" + token + ". Should look like '35:20'");
      }
      double lat = Double.parseDouble(token.substring(0, colon));
      double lng = Double.parseDouble(token.substring(colon + 1));
      S2LatLng ll = S2LatLng.fromDegrees(lat, lng);
      S2Point vertex = ll.toPoint();
      if (level >= 0) {
        vertex = snapPointToLevel(vertex, level);
      }
      vertices.add(vertex);
      builder.addPoint(ll);
    }

    return builder.build();
  }

  static S2Point snapPointToLevel(S2Point point, int level) {
    return S2CellId.fromPoint(point).parent(level).toPoint();
  }

  static List<S2Point> snapPointsToLevel(List<S2Point> points, int level) {
    List<S2Point> snapped = new ArrayList<>(points.size());
    for (S2Point p : points) {
      snapped.add(snapPointToLevel(p, level));
    }
    return snapped;
  }

  static S2Point makePoint(String str) {
    List<S2Point> vertices = Lists.newArrayList();
    parseVertices(str, vertices);
    return Iterables.getOnlyElement(vertices);
  }

  /**
   * Given a string of latitude-longitude coordinates in degrees, returns a newly allocated loop.
   * Example of the input format: {@code -20:150, 10:-120, 0.123:-170.652}.
   *
   * <p>The exact strings "empty" or "full" create the empty or full loop, respectively.
   */
  static S2Loop makeLoop(String str) {
    return makeLoop(str, -1);
  }

  /** Like makeLoop() above, but snap the vertices of the resultant loop to the given 'level'. */
  static S2Loop makeLoop(String str, int level) {
    if ("empty".equalsIgnoreCase(str)) {
      return S2Loop.empty();
    } else if ("full".equalsIgnoreCase(str)) {
      return S2Loop.full();
    }
    List<S2Point> vertices = Lists.newArrayList();
    parseVertices(str, vertices, level);
    return new S2Loop(vertices);
  }

  /** Convert a distance on the Earth's surface to an angle. */
  static S1Angle kmToAngle(double km) {
    return metersToAngle(1000 * km);
  }

  /** Convert a distance on the Earth's surface to an angle. */
  static S1Angle metersToAngle(double meters) {
    return S1Angle.radians(meters / EARTH_RADIUS_METERS);
  }

  /**
   * Given a sequence of loops separated by semicolons, returns a newly allocated polygon. Loops are
   * automatically normalized by inverting them if necessary so that they enclose at most half of
   * the unit sphere.
   *
   * <p>Historically this was once a requirement of polygon loops. It also hides the problem that if
   * the user thinks of the coordinates as X:Y rather than LAT:LNG, it yields a loop with the
   * opposite orientation.
   *
   * <p>Examples of the input format:
   *
   * <ul>
   *   <li>"10:20, 90:0, 20:30" <=> one loop
   *   <li>"10:20, 90:0, 20:30; 5.5:6.5, -90:-180, -15.2" <=> >two loops
   *   <li>"" <=> the empty string results in the empty polygon (consisting of no loops)
   *   <li>"full" <=> the full polygon (consisting of one full loop)
   *   <li>"empty" <=> **INVALID** the empty polygon has no loops.
   * </ul>
   */
  public static S2Polygon makePolygon(String str) {
    return internalMakePolygon(str, true, -1);
  }

  /** Like makePolygon() above, but snaps the polygon to the specified level. */
  static S2Polygon makePolygon(String str, int level) {
    return internalMakePolygon(str, true, level);
  }

  /**
   * Like makePolygon(), except that it does not normalize loops (i.e., it gives you exactly what
   * you asked for).
   */
  static S2Polygon makeVerbatimPolygon(String str) {
    return internalMakePolygon(str, false, -1);
  }

  private static final S2Polygon internalMakePolygon(
      String str, boolean normalizeLoops, int level) {
    List<S2Loop> loops = Lists.newArrayList();
    if (str.trim().equals("empty")) {
      return new S2Polygon();
    } else if (str.trim().equals("full")) {
      loops.add(S2Loop.full());
      return new S2Polygon(loops);
    }

    for (String token : Splitter.on(';').omitEmptyStrings().split(str)) {
      S2Loop loop = makeLoop(token, level);
      if (normalizeLoops) {
        loop.normalize();
      }
      loops.add(loop);
    }

    return new S2Polygon(loops);
  }

  static S2Polyline makePolyline(String str) {
    List<S2Point> vertices = Lists.newArrayList();
    parseVertices(str, vertices);
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new S2Polyline(vertices).encode(bos);
      return S2Polyline.decode(new ByteArrayInputStream(bos.toByteArray()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns an S2ShapeIndex containing the points, polylines, and loops (in the form of a single
   * polygon) described by the following format:
   *
   * <pre>point1|point2|... # line1|line2|... # polygon1|polygon2|...</pre>
   *
   * <p>Examples:
   *
   * <table>
   * <tr><td>Two points</td><td><pre>1:2 | 2:3 # #</pre></td></tr>
   * <tr><td>Two polylines</td><td><pre># 0:0, 1:1, 2:2 | 3:3, 4:4 #</pre></td></tr>
   * <tr><td>Shell with hole</td><td><pre># # 0:0, 0:3, 3:0; 1:1, 2:1, 1:2</pre></td></tr>
   * <tr><td>One of each</td><td><pre>5:5 # 6:6, 7:7 # 0:0, 0:1, 1:0</pre></td></tr>
   * </table>
   *
   * <p>Loops should be directed so that the region's interior is on the left.
   */
  S2ShapeIndex makeIndex(String str) {
    S2ShapeIndex index = new S2ShapeIndex();
    List<String> strs = ImmutableList.copyOf(Splitter.on('#').split(str));
    Preconditions.checkArgument(3 == strs.size(), "Must contain two # characters: %s", str);
    List<S2Point> points = new ArrayList<>();
    for (String point : Splitter.on('|').omitEmptyStrings().split(strs.get(0).trim())) {
      points.add(makePoint(point));
    }
    if (!points.isEmpty()) {
      index.add(S2Point.Shape.fromList(points));
    }
    for (String line : Splitter.on('|').omitEmptyStrings().split(strs.get(1).trim())) {
      index.add(makePolyline(line));
    }
    for (String polygon : Splitter.on('|').omitEmptyStrings().split(strs.get(2).trim())) {
      // TODO(eengle): Use LaxPolygon after it is added.
      index.add(makePolygon(polygon).shape());
    }
    return index;
  }

  /** Returns the result of encoding and immediately decoding the given value. */
  @SuppressWarnings("unchecked")
  @GwtIncompatible("ByteArrayInputStream")
  static <E> E encodeDecode(Serializable value) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new ObjectOutputStream(bytes).writeObject(value);
    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    return (E) in.readObject();
  }

  /**
   * Returns a pair of nested loops, such that the second loop in the returned list is nested within
   * the first. Both loops are centered at 'p', and each has 'numVertices' vertices. The outer loop
   * has the given radius 'outerRadius'. The inner loop is inset by a small distance ('gap') from
   * the outer loop which is approximately equal to 'gapEdgeMultiple' times the edge length of the
   * outer loop. (This allows better spatial indexing, which becomes less effective at pruning
   * intersection candidates as the loops get closer together.)
   *
   * <p>Caveats: The gap is actually measured to the incircle of the outer loop, and the gap is
   * clamped if necessary to prevent the inner loop from becoming vanishingly small. (Rule of thumb:
   * to obtain a 'gapEdgeMultiple' of 'm', the loops must have approximately 7 * m vertices or more.
   */
  static List<S2Loop> makeNestedLoopPair(
      S1Angle outerRadius, double gapEdgeMultiple, int numVertices, S2Point p) {
    // The inner loop is inscribed within the incircle (maximum inscribed circle) of the outer
    // loop.
    S1Angle incircleRadius =
        S1Angle.radians(outerRadius.radians() * Math.cos(Math.PI / numVertices));
    S1Angle edgeLen = S1Angle.radians(outerRadius.radians() * (2 * Math.PI / numVertices));

    // If the edge count is too small, it may not be possible to inset the inner loop by the given
    // multiple of the edge length.  We handle this by clamping 'innerRadius' to be at least 1% of
    // 'outerRadius'.
    S1Angle innerRadius =
        S1Angle.radians(
            Math.max(
                incircleRadius.radians() - gapEdgeMultiple * edgeLen.radians(),
                0.01 * incircleRadius.radians()));

    // Generate two loops with the same center.
    return Arrays.asList(
        S2Loop.makeRegularLoop(p, outerRadius, numVertices),
        S2Loop.makeRegularLoop(p, innerRadius, numVertices));
  }

  /**
   * Returns a pair of crossing loops. The first loop in the returned list will have center 'aPoint'
   * and radius 'aRadius'. The second will have its center along the arc containing aPoint and
   * bPoint, and it will have radius 'bRadius'. Both loops have 'numVertices' vertices.
   */
  List<S2Loop> makeCrossingLoopPair(
      S1Angle aRadius, S1Angle bRadius, int numVertices, S2Point aPoint, S2Point bPoint) {
    // The edges of each loop are bounded by two circles, one circumscribed around the loop (the
    // circumcircle), and the other inscribed within the loop (the incircle).  Our strategy is to
    // place the smaller loop such that its incircle crosses both circles of the larger loop.
    double maxRadius = Math.max(aRadius.radians(), bRadius.radians());
    double minRadius = Math.min(aRadius.radians(), bRadius.radians());

    // Check that the smaller loop is big enough that its incircle can span the gap between the
    // incircle and the circumcircle of the larger loop. The incircle factor is the loop radius
    // divided by its incircle radius.
    double incircleFactor = Math.cos(Math.PI / numVertices);
    assertTrue(minRadius * incircleFactor > maxRadius * (1 - incircleFactor));

    // Compute the range of distances between the two loop centers such that the incircle of the
    // smaller loop crosses both circles of the larger loop.
    double minDist = maxRadius - incircleFactor * minRadius;
    double maxDist = incircleFactor * (minRadius + maxRadius);

    // Now generate a pair of loops whose centers are separated by distances in the given range.
    // Loop orientations are chosen randomly.
    Random rand = new Random(0);
    S1Angle angle = S1Angle.radians(minDist + rand.nextDouble() * (maxDist - minDist));
    S2Point bCenter = S2EdgeUtil.interpolateAtDistance(angle, aPoint, bPoint);
    return Arrays.asList(
        S2Loop.makeRegularLoop(aPoint, aRadius, numVertices),
        S2Loop.makeRegularLoop(bCenter, bRadius, numVertices));
  }

  /**
   * Returns the pair of crossing loops given by getCrossingLoopPair() when using default values for
   * the radii of the two loops. The loops will have centers 'aPoint' and 'bPoint', and will have
   * 'numVertices' vertices each.
   */
  List<S2Loop> makeCrossingLoopPairDefault(int numVertices, S2Point aPoint, S2Point bPoint) {
    S1Angle aRadius = DEFAULT_RADIUS;
    S1Angle bRadius = S1Angle.radians(aRadius.radians() * DEFAULT_CROSSING_RADIUS_RATIO);
    return makeCrossingLoopPair(aRadius, bRadius, numVertices, aPoint, bPoint);
  }

  /**
   * Returns a pair of disjoint loops . The loops are constructed so that it is impossible to
   * determine the relationship between them based solely on their bounds (they could be nested,
   * crossing, or disjoint). The outer loop (1st loop in returned list) will look somewhat like the
   * outline of the letter "C": it consists of two nested loops (the "outside shell" and the "inside
   * shell"), which each have a single edge removed and are then joined together to form a single
   * loop. The inner loop (2nd loop in returned list) is then nested within the inside shell of the
   * outer loop.
   *
   * <p>The outer loop has 'numVertices' vertices split between its outside and inside shells. The
   * radius of the outside shell is 'outerRadius', while the radius of the inside shell is (0.9 *
   * outerRadius).
   *
   * <p>The inner loop has 'numVertices' vertices, and is separated from the inside shell of the
   * outer loop by a small distance ("gap") which is approximately equal to 'gapMultipleEdges' times
   * the edge length of the inside shell. (See getNestedLoopPair for details.)
   */
  static List<S2Loop> makeDisjointLoopPair(
      S1Angle outerRadius, double gapEdgeMultiple, int numVertices, S2Point p) {
    // Compute the radius of the inside shell of the outer loop, the edge length of the outer
    // shell, and finally the incircle radius of the inside shell (this is the maximum possible
    // radius of the inner loop).
    S1Angle outerInsideRadius =
        S1Angle.radians(0.9 * outerRadius.radians() * Math.cos(2 * Math.PI / numVertices));
    S1Angle edgeLen = S1Angle.radians(outerInsideRadius.radians() * (Math.PI / numVertices));
    S1Angle incircleRadius =
        S1Angle.radians(outerInsideRadius.radians() * Math.cos(2 * Math.PI / numVertices));

    // See comments in getNestedLoopPair().
    S1Angle innerRadius =
        S1Angle.radians(
            Math.max(
                incircleRadius.radians() - gapEdgeMultiple * edgeLen.radians(),
                0.01 * incircleRadius.radians()));

    S2Loop outerOutside = S2Loop.makeRegularLoop(p, outerRadius, Math.max(4, numVertices / 2));
    S2Loop outerInside = S2Loop.makeRegularLoop(p, outerInsideRadius, Math.max(4, numVertices / 2));
    List<S2Point> vertices =
        Lists.newArrayListWithCapacity(outerInside.numVertices() + outerOutside.numVertices());

    // Join together the outside and inside shells to form the outer loop.
    for (int j = outerInside.numVertices() - 1; j >= 0; j--) {
      vertices.add(outerInside.vertex(j));
    }
    for (int j = 0; j < outerOutside.numVertices(); ++j) {
      vertices.add(outerOutside.vertex(j));
    }

    return Arrays.asList(new S2Loop(vertices), S2Loop.makeRegularLoop(p, innerRadius, numVertices));
  }

  /**
   * Returns {@code numIds} S2 cell IDs at level {@link S2CellId#MAX_LEVEL}, evenly spread over the
   * range of valid cells at that level.
   */
  static S2CellId[] generateEvenlySpacedIds(int numIds) {
    S2CellId[] ids = new S2CellId[numIds];
    long start = S2CellId.begin(S2CellId.MAX_LEVEL).id();
    long end = S2CellId.end(S2CellId.MAX_LEVEL).id();
    long delta = UnsignedLongs.divide(end - start, numIds);
    for (int i = 0; i < numIds; i++) {
      ids[i] = new S2CellId(start + i * delta);
    }
    return ids;
  }

  /**
   * Asserts that the first N calls to {@link S2Shape#getEdge} returns edges are equivalent to those
   * specified in the edge list, described by the following format:
   *
   * <pre>edge1, edge2, ..., edgeN</pre>
   *
   * where edges are:
   *
   * <pre>point1 | point2</pre>
   *
   * and points are:
   *
   * <pre>lat:lng</pre>
   *
   * <p>Example:
   *
   * <table>
   * <tr><td>Two edges</td><td><pre>1:2 | 2:3, 4:5 | 6:7</pre></td></tr>
   * </table>
   */
  static void checkFirstNEdges(S2Shape shape, String edgeList) {
    MutableEdge result = new MutableEdge();
    consumeIndexedEdges(
        edgeList,
        (int edgeId, MutableEdge expected) -> {
          shape.getEdge(edgeId, result);
          assertEquals(expected.getStart(), result.getStart());
          assertEquals(expected.getEnd(), result.getEnd());
        });
  }

  /**
   * Similar to {@link #checkFirstNEdges}, except that {@link S2Shape#getChainEdge} is used with
   * specified chain id to return the edges.
   */
  static void checkFirstNChainEdges(S2Shape shape, int chainId, String edgeList) {
    MutableEdge result = new MutableEdge();
    consumeIndexedEdges(
        edgeList,
        (int offset, MutableEdge expected) -> {
          shape.getChainEdge(chainId, offset, result);
          assertEquals(expected.getStart(), result.getStart());
          assertEquals(expected.getEnd(), result.getEnd());
        });
  }

  private interface IndexedEdgeConsumer {
    void apply(int index, MutableEdge edge);
  }

  private static void consumeIndexedEdges(String edgeStrList, IndexedEdgeConsumer consumer) {
    MutableEdge edge = new MutableEdge();
    int i = 0;
    for (String edgeStr : Splitter.on(',').split(edgeStrList)) {
      int j = edgeStr.indexOf('|');
      edge.set(
          makePoint(edgeStr.substring(0, j).trim()), makePoint(edgeStr.substring(j + 1).trim()));
      consumer.apply(i++, edge);
    }
  }

  /** Asserts that the first N chain starts are equivalent to those specified in the list. */
  static void checkFirstNChainStarts(S2Shape shape, int... starts) {
    int chainId = 0;
    for (int start : starts) {
      assertEquals(start, shape.getChainStart(chainId++));
    }
  }

  /** Asserts that the first N chain lengths are equivalent to those specified in the list. */
  static void checkFirstNChainLengths(S2Shape shape, int... lengths) {
    int chainId = 0;
    for (int length : lengths) {
      assertEquals(length, shape.getChainLength(chainId++));
    }
  }
}
