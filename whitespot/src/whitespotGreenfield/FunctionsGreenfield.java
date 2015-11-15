package whitespotGreenfield;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FunctionsGreenfield {
	static LocationContainer locationContainer;
	static PolygonContainer polygonContainer;
	
	public static void getPolygonContainer(){
		polygonContainer = FunctionsCommon.getPolygonContainer();
	}
	
	public static void getLocationContainer (){
		locationContainer=FunctionsCommon.getLocationContainer();
	}
	
	private static void setPolygonContainer(){
		FunctionsCommon.setPolygonContainer(polygonContainer);
	}
	
	private static void setLocationContainer(){
		FunctionsCommon.setLocationContainer(locationContainer);
	}
	
	private static void Getters(){
		getPolygonContainer();
		getLocationContainer();
	}
	
	private static void Setters(){
		setPolygonContainer();
		setLocationContainer();
	}
	
	public static void allocatePolygonsGreenfield(int numberpolygons,
			int numberlocations, boolean PLZ5) throws SQLException, Exception {

		Getters();
		
		// detect startPolys on Boundary
		List<Integer> boundaryPolyIds = new ArrayList<Integer>();
		boundaryPolyIds = FunctionsGreenfieldWhitespot.getBoundaryPolys(PLZ5);

		List<Integer> allocatedPolyIds = new ArrayList<Integer>();
		double critAverage = -1;

		// get sum of Polygons to calculate a critAverage value to stop
		// allocation of polys for one location
		double sumCriteria=FunctionsGreenfieldWhitespot.getCritSum(numberpolygons);
		
		critAverage=FunctionsGreenfieldWhitespot.calculateCritaverage(PLZ5, sumCriteria, numberlocations);
		
		int sumOfPolygons = 0;
		double oldCrit = 0;

		// create locations and allocate Polygons to it
		for (int i = 0; i < numberlocations; i++) {

			Location old = null;

			if (i > 0) {
				old = locationContainer.getLocationByID(i);
			}

			double actcrit = 0;

			// getStartPoly
			Polygon startPoly = null;
			if (i == 0) {
				startPoly = polygonContainer.getPolygonById(numberpolygons,
						boundaryPolyIds.get(0));
			} else {
				// detect startPoly, startpoly is a boundary poly neighbourd to
				// last location
				int j = 0;
				boolean found = false;

				while (j < numberpolygons && !found) {
					if (polygonContainer.getPolygon(j)
							.getFlagAllocatedLocation()) {
						if (polygonContainer.getPolygon(j)
								.getAllocatedLocation().getId() == old.getId()) {
							List<Polygon> neighbours = polygonContainer
									.getPolygon(j).getNeighbours();

							for (int k = 0; k < neighbours.size(); k++) {
								for (int l = 0; l < boundaryPolyIds.size(); l++) {
									if (neighbours.get(k).getId() == boundaryPolyIds
											.get(l)
											&& !polygonContainer
													.getPolygonById(
															numberpolygons,
															boundaryPolyIds
																	.get(l))
													.getFlagAllocatedLocation()) {
										found = true;
										startPoly = polygonContainer
												.getPolygonById(numberpolygons,
														boundaryPolyIds.get(l));
									}
								}
							}

							j++;
						} else {
							j++;
						}
					} else {
						j++;
					}
				}
			}

			// if no boundaryPoly is available anymore; a polygon within the
			// whole area will be taken
			if (startPoly == null) {
				int j = 0;
				boolean found = false;

				while (j < numberpolygons && !found) {
					if (polygonContainer.getPolygon(j)
							.getFlagAllocatedLocation()) {
						if (polygonContainer.getPolygon(j)
								.getAllocatedLocation().getId() == old.getId()) {
							List<Polygon> neighbours = polygonContainer
									.getPolygon(j).getNeighbours();

							for (int k = 0; k < neighbours.size(); k++) {
								if (!neighbours.get(k)
										.getFlagAllocatedLocation()) {
									found = true;
									startPoly = polygonContainer
											.getPolygonById(numberpolygons,
													neighbours.get(k).getId());
								}
							}

							if (!found) {
								j++;
							}
						} else {
							j++;
						}
					} else {
						j++;

					}
				}
			}

			// if no neighbour polygone is possible
			if (startPoly == null) {
				if (boundaryPolyIds.size() > 0) {
					for (int j = 0; j < boundaryPolyIds.size(); j++) {
						if (!allocatedPolyIds.contains(boundaryPolyIds.get(j))) {
							startPoly = polygonContainer.getPolygonById(
									numberpolygons, boundaryPolyIds.get(j));
						}
					}
				} else {
					for (int j = 0; j < numberpolygons; j++) {
						if (!allocatedPolyIds.contains(polygonContainer
								.getPolygon(j).getId())) {
							startPoly = polygonContainer.getPolygon(j);
						}
					}
				}
			}

			// set variables for startPoly
			locationContainer.add(i + 1);
			Location loc = locationContainer.getLocationByID(i + 1);
			startPoly.setAllocatedLocation(loc);
			loc.setAllocatedPolygon(startPoly);
			actcrit = startPoly.getCriteria();
			double critThreshold = -1;
			if (i == 0) {
				critThreshold = critAverage;
			} else {
				critThreshold = 2 * critAverage - oldCrit;
				if (critThreshold > critAverage) {
					critThreshold = critAverage;
				}
			}

			// init Distances
			FunctionsGreenfieldWhitespot.initDistancesToCentroids(numberpolygons, startPoly);

			sumOfPolygons++;
			List<Integer> buffAllocatedPolyIds = new ArrayList<Integer>();
			buffAllocatedPolyIds.add(startPoly.getId());
			if (i == (numberlocations - 1)) {
				critThreshold = critAverage + oldCrit;
			}

			boolean takeNextLoc = false;
			int runs = 0;

			while (actcrit < critThreshold
					&& sumOfPolygons != (numberpolygons - (numberlocations - i))
					&& !takeNextLoc && runs != numberpolygons) {

				// detect Polygon with minimal distance
				Polygon minPoly = null;
				boolean minPolyfound = false;
				for (int k = 0; k < numberpolygons; k++) {
					if (!polygonContainer.getPolygon(k)
							.getFlagAllocatedLocation()) {
						boolean unit = FunctionsCommon.checkUnitCalculationGets(
								polygonContainer.getPolygon(k).getId(),
								loc.getId(), numberpolygons);
						if (unit) {
							minPoly = polygonContainer.getPolygon(k);
							minPolyfound = true;
						}
					}
				}

				if (minPolyfound) {
					for (int k = 0; k < numberpolygons; k++) {
						Polygon actPoly = polygonContainer.getPolygon(k);
						if (!actPoly.getFlagAllocatedLocation()
								&& actPoly.getId() != startPoly.getId()) {
							if (actPoly.getDistance() < minPoly.getDistance()) {
								boolean unit = FunctionsCommon.checkUnitCalculationGets(
										actPoly.getId(), loc.getId(),
										numberpolygons);
								if (unit) {
									minPoly = actPoly;
								}
							}
						}
					}

					// allocate polygon
					loc.setAllocatedPolygon(minPoly);
					minPoly.setAllocatedLocation(loc);
					actcrit = actcrit + minPoly.getCriteria();
					buffAllocatedPolyIds.add(minPoly.getId());
					sumOfPolygons++;
				} else { // if no neighbour polygon is possible anymore
					takeNextLoc = true;
				}
			}

			System.out.println(actcrit);
			for (int j = 0; j < buffAllocatedPolyIds.size(); j++) {
				allocatedPolyIds.add(buffAllocatedPolyIds.get(j));
			}
			loc.setCriteria(actcrit);
			oldCrit = actcrit;
		}
		
		Setters();
	}

	

	public static void calculateGreenfieldLocations(int numberpolygons,
			int numberlocations, boolean PLZ5) throws SQLException {

		Getters();
		
		for (int i = 0; i < numberlocations; i++) {
				double[] coordinates = new double[2];
				
				coordinates=FunctionsGreenfieldWhitespot.calculateLocations(numberpolygons, PLZ5, i);

				locationContainer.setLonLat(coordinates[0],
						coordinates[1], i);
		}
		
		Setters();
	}
	
}
