package whitespotGreenfield;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.FileReader;

public class FunctionsCommon {
	static LocationContainer locationContainer;
	static PolygonContainer polygonContainer;
	static int counterIdUsed = 0;
	static int lastPolyID;
	static boolean nofoundBiggest = false;
	static boolean nofoundSmallest = false;
	static int nofoundlocbiggest = -1;
	static int nofoundlocsmallest = -1;
	static List<Integer> nofoundlocations = new ArrayList<Integer>();

	private static class polyDistances {
		static List<Integer> ids = new ArrayList<Integer>();
		static ArrayList<Double>[] distances;
	}
	
	//setter and getter functions
	public static void setPolygonContainer(PolygonContainer polyCon){
		polygonContainer=polyCon;
	}
	
	public static void setLocationContainer(LocationContainer locCon){
		locationContainer=locCon;
	}
	
	public static PolygonContainer getPolygonContainer(){
		return polygonContainer;
	}
	
	public static LocationContainer getLocationContainer(){
		return locationContainer;
	}
	
	//three main functions:initialisation, area segmentation and visulisation --> are used by all three algorithm
	
	/**calls functions for initialisation
	 * @param numberlocations: number of used locations
	 * @param number: true or false, dependent on decision whether number (true) or sum (false) should be given back calling getNrOrSum
	 * @param PLZ5: true or false to show which data
	 */
	public static int initialisation(int numberlocations, boolean number, boolean PLZ5, boolean microm) throws Exception, SQLException{
		
		int numberpolygons= getNrOrSum(number, PLZ5, microm);
		
		initPolygones(numberpolygons, numberlocations, PLZ5, microm);
		
		initNeighbours(numberpolygons, PLZ5, microm);
		
		initCentroids(numberpolygons);
		
		initArea(numberpolygons, PLZ5, microm);
		
		initCircumferences(numberpolygons, PLZ5, microm);
		
		return numberpolygons;
	}
	
public static void areaSegmentation(int numberpolygons, int numberlocations, boolean PLZ5, boolean microm, int threshold, int weightCom, int weightCrit) throws Exception{
		areaSegmentation(numberpolygons, numberlocations, PLZ5, microm, threshold, weightCom, weightCrit, false, -1);
	}
	
	/** calls all functions that are necessary for area segmentation
	 * @param numberpolygons: number of geometries in the calculation areas
	 * @param numberlocations: number of used locations
	 * @param PLZ5: true or false
	 * @param threshold: int value of threshold for rearranging to get balanced criteria
	 * @param weightCom: int value for weight of compactness: used during rearranging
	 * @param weightCrit: int value for weight of criteria: used during rearranging
	 * @param whitespot: true or false
	 * @param numberGivenLocations: just necessary for whitespot
	 * @param numberNewLocations: just necessary for whitespot
	 */
	public static void areaSegmentation(int numberpolygons, int numberlocations, boolean PLZ5, boolean microm, int threshold, int weightCom, int weightCrit, boolean whitespot, int numberGivenLocations) throws Exception{
		determineHomePoly(PLZ5, numberlocations, microm);
		
		initDistances(numberpolygons, numberlocations, microm);
		
		allocatePolygonsByDistance(numberpolygons, numberlocations);
		System.out.println("init by distance done");
		
		initCriteria(numberpolygons, numberlocations);
		System.out.println("init Criteria done");
		
		FileWriter output2 =FunctionsCommon.createFileWriter();
		 FunctionsCommon.writePolygon(output2, numberpolygons);
		 output2.close();
		 
//		checkUnityAfterAllocByDist(numberpolygons, numberlocations);
		System.out.println("check done");
		
//		output2 =FunctionsCommon.createFileWriter("debug");
//		 FunctionsCommon.writePolygon(output2, numberpolygons);
//		 output2.close();
		 
		 System.out.println("starting rearrangement");
		checkThreshold(numberpolygons, numberlocations, threshold, microm, PLZ5, weightCom, weightCrit, whitespot, numberGivenLocations);
	}
	
	/**calls functions for visualisation of the results
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 * @param output: File for saving output
	 */
	public static void visualizeResults(int numberpolygons, int numberlocations, FileWriter output) throws Exception{
		writePolygon(output, numberpolygons);
		
		showCritResult(numberlocations);
	}

	//----------------Initialisation------------------------
	
	/**calculates the number of given geometries Or the sum of the whole criterias
	 * @param number: boolean, true if number should be calculated, false for calculation of criteria sum
	 * @param PLZ5: boolean, to indicate area
	 * @return number or sum
	 */
	private static int getNrOrSum(boolean number, boolean PLZ5, boolean microm)
			throws SQLException, ClassNotFoundException {

		//initialise variables
		Connection jdbc = null;
		Statement stmt = null;

		//get Connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//choose table
		StringBuffer sb = new StringBuffer();
		String table = null;
		if (PLZ5) {
			table = "criteriasplz51";
		} else {
			table = "criteriasplz81";
		}

		//create SQL statement
		if (number) {
			sb.append("SELECT COUNT (id) FROM " + table + ";");
		} else {
			sb.append("SELECT SUM(CAST(_c1 AS int)) FROM " + table + ";");
		}

		//execute Query
		ResultSet t = null;
		if (!microm) {
			t = stmt.executeQuery(sb.toString());
		}

		//save and return result
		t.next();
		int sum = t.getInt(1);
		if (number) {
			System.out.println("numberofpolygons: " + sum);
		} else {
			System.out.println("sum: " + sum);
		}

		if (jdbc != null) {
			jdbc.close();
		}

		return sum;
	}
	
	/**initialisation of polygones getting id, geometrie, critvalue etc from databse and store it into local variables
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 * @param PLZ5: boolean to indicate databse
	 */
	private static void initPolygones(int numberpolygons, int numberlocations,
			boolean PLZ5, boolean microm) throws SQLException {

		//initialise variables
		Connection jdbc = null;
		Statement stmt = null;

		//create connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//init database table and columns
		String columnIDs = null;
		String tablegeom = null;
		String tablecrit = null;
		// PLZ5
		if (PLZ5) {
			columnIDs = "_g7304";
			tablegeom = "geometriesplz51";
			tablecrit = "criteriasplz51";
		} else {
			// PLZ8
			columnIDs = "_g7305";
			tablegeom = "geometriesplz81";
			tablecrit = "criteriasplz81";
		}

		//create SQL statement --> get all PolygonIDs, geometries and critvalues and store it
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM "
				+ tablegeom
				+ " AS t1 INNER JOIN "
				+ tablecrit
				+ " AS t2 ON t2." + columnIDs + "=t1.id");
		System.out.println(sb);

		ResultSet t = null;

		//execute query
		if (!microm) {
			t = stmt.executeQuery(sb.toString());
		}

		//init polygonContainer for saving polygones; contains instances of polygons
		polygonContainer = new PolygonContainer();

		// get ids, geometry and criteria
		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			t.next();
			int id = t.getInt("id");
			String geometry = t.getString("the_geom");
			double criteria = t.getDouble("criteria");

			polygonContainer.add(id, geometry, criteria);
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}
	
	/**initialise the neighbour polygones of every geometrie
	 * @param numberpolygons: number of geometries in calculation area
	 * @param PLZ5: boolean, indicates table
	 */
	private static void initNeighbours(int numberpolygons, boolean PLZ5,
			boolean microm) throws SQLException {

		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//create connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//init table and collumns
		String tablegeom = null;
		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		//getting nearest neighbours for every geometrie and store them
		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			List<Integer> neighbours = new ArrayList<Integer>();
			Polygon poly = polygonContainer.getPolygon(i);
			ResultSet nN = null;
			
			//calculate Nearest neighbours
			if (!microm) {
				nN = getNearestNeighbours(poly.getId(), tablegeom, stmt);
			}

			//store neighbours
			boolean last = false;
			while (!last) {
				nN.next();
				//store just real neighbours, not polygone itself
				if (nN.getInt(1)!=poly.getId()){
					neighbours.add(nN.getInt(1));
				}
				if (nN.isLast()) {
					last = true;
				}
			}

			//get polygon objects from neighbours to store polygone instance and not only id
			List<Polygon> neighbourPolys = new ArrayList<Polygon>();

			for (int j = 0; j < neighbours.size(); j++) {
				boolean found = false;
				int pos = 0;

				while (!found) {
					Polygon actPoly = polygonContainer.getPolygon(pos);
					if (actPoly.getId() == neighbours.get(j)) {
						found = true;
						neighbourPolys.add(actPoly);
					} else {
						pos++;
					}

					if (pos > numberpolygons) {
						found = true;
					}
				}
			}

			//set neighbour polygone instances--> List<Polygones>
			poly.setNeighbours(neighbourPolys);
		}

		if (jdbc != null) {
			jdbc.close();
		}
	}
	
	/**creates the SQL statement for calculation of neighbours
	 * @param polyID: id of geometrie for that neighbours should be calculated
	 * @param tablegeom: name of table where geometries are stored
	 * @param jdbc: connection to database
	 * @return ResultSet which contains the neighbours of the polygon
	 * @throws SQLException
	 * attention: PostGIS function gives also polygon with polyID as neighbour back
	 */
	private static ResultSet getNearestNeighbours(int polyID, String tablegeom,
			Statement jdbc) throws SQLException {
		// SELECT (pgis_fn_nn(p1.the_geom, 0.0005, 1000, 10, 'geometries',
		// 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT
		// st_geomfromtext((Select st_astext(the_geom) FROM geometries WHERE
		// ID=1), 4326) AS the_geom) AS p1;
		
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT (pgis_fn_nn(p1.the_geom, 0.0, 1000, 10, '"
				+ tablegeom
				+ "', 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT st_geomfromtext((Select st_astext(the_geom) FROM "
				+ tablegeom + " WHERE ID=" + polyID
				+ "), 4326) AS the_geom) AS p1;");
		ResultSet rNeighbours = jdbc.executeQuery(sb.toString());

		return rNeighbours;
	}
	
	/**initialise centroid points of given geometries and store them local
	 * @param numberpolygons: number of geometries in calculation area
	 * @throws SQLException
	 */
	public static void initCentroids(int numberpolygons) throws SQLException {
		//getting centroid for every polygone and store it
		for (int i = 0; i < numberpolygons; i++) {
			String geom = polygonContainer.getPolygon(i).getGeometry();
			
			//calculate centroid: Point geometry as String is given back
			String centroid = calculateCentroid(geom);

			System.out.println(centroid);
			//parse centroid to store point with Longitude and Latitude
			int posBracket = centroid.indexOf("(");
			int posSpace = centroid.indexOf(" ");
			double lon = Double.parseDouble(centroid.substring(posBracket + 1,
					posSpace));
			double lat = Double.parseDouble(centroid.substring(posSpace + 1,
					centroid.length() - 1));

			//setCentroid
			polygonContainer.getPolygon(i).setCentroid(lon, lat);
		}
	}
	
	/**calculates centroid and gives result back
	 * @param geometry: geometry for which centroid should be caluclated
	 * @return centroid point as string
	 * @throws SQLException
	 */
	public static String calculateCentroid(String geometry) throws SQLException {
		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//get connection
		jdbc = getConnection();
		stmt = jdbc.createStatement();

		//create SQL statement
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ST_AsText(st_centroid(ST_GeomFromText('" + geometry
				+ "')));");
		
		//execute Query and store result 
		ResultSet d = stmt.executeQuery(sb.toString());
		d.next();
		String centroid = d.getString(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return centroid;
	}
	
	private static void initArea(int numberpolygons, boolean PLZ5, boolean microm) throws SQLException{
		//init variables
				Connection jdbc = null;
				Statement stmt = null;

				//init connection
				if (!microm) {
					jdbc = getConnection();
					stmt = jdbc.createStatement();
				}

				//set table and columns
				String tablegeom = null;

				// PLZ5
				if (PLZ5) {
					tablegeom = "geometriesplz51";
				} else {
					// PLZ8
					tablegeom = "geometriesplz81";
				}

				for (int i=0;i<numberpolygons;i++){
					Polygon actPoly = polygonContainer.getPolygon(i);
					
					StringBuffer sb = new StringBuffer();
					List<Integer> geomIDs = new ArrayList<Integer>();
					geomIDs.add(actPoly.getId());

					//formate String for SQL statement
					StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
					idsBuffer.deleteCharAt(0);
					idsBuffer.deleteCharAt(idsBuffer.length() - 1);

					//create SQL statement
					sb = calculateArea(tablegeom, idsBuffer);

					//execute query and store result
					ResultSet d = null;
					if (!microm) {
						d = stmt.executeQuery(sb.toString());
					}

					d.next();

					double result = d.getDouble(1);
					
					actPoly.setArea(result);
				}
				

				if (jdbc != null) {
					jdbc.close();
				}
	}
	
	private static void initCircumferences(int numberpolygons, boolean PLZ5, boolean microm) throws SQLException{
		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//init connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//set table and columns
		String tablegeom = null;

		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		//calculate circumference for each geometry
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly = polygonContainer.getPolygon(i);
			
			StringBuffer sb = new StringBuffer();
			List<Integer> geomIDs = new ArrayList<Integer>();
			geomIDs.add(actPoly.getId());

			//formate String for SQL statement
			StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
			idsBuffer.deleteCharAt(0);
			idsBuffer.deleteCharAt(idsBuffer.length() - 1);

			//create SQL statement
			sb = calculateCircumference(tablegeom, idsBuffer);
//			sb.append("SELECT ST_Length(ST_CollectionExtract(ST_Intersection(a_geom, b_geom), 2)) ");
//			sb.append("FROM (SELECT (SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(0)+") AS a_geom,");
//			sb.append("(SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(1)+") AS b_geom) f;");

			//execute query and store result
			ResultSet d = null;
			if (!microm) {
				d = stmt.executeQuery(sb.toString());
			}

			d.next();

			double result = d.getDouble(1);
			
			actPoly.setCircumference(result);
		}
		
		//calculate shared edge with neighboured polygones
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly=polygonContainer.getPolygon(i);
			
			for (int j=0;j<actPoly.getNeighbours().size();j++){
				Polygon neighbourPoly=actPoly.getNeighbours().get(j);
				
				StringBuffer sb = new StringBuffer();
				List<Integer> geomIDs = new ArrayList<Integer>();
				geomIDs.add(actPoly.getId());
				geomIDs.add(neighbourPoly.getId());

				//formate String for SQL statement
				StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
				idsBuffer.deleteCharAt(0);
				idsBuffer.deleteCharAt(idsBuffer.length() - 1);

				//create SQL statement
//				sb = calculateCircumference(tablegeom, idsBuffer);
				sb.append("SELECT ST_Length(ST_CollectionExtract(ST_Intersection(a_geom, b_geom), 2)) ");
				sb.append("FROM (SELECT (SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(0)+") AS a_geom,");
				sb.append("(SELECT the_geom from "+tablegeom+" where id="+geomIDs.get(1)+") AS b_geom) f;");
				
				System.out.println(sb);
				ResultSet d = null;
				if (!microm) {
					d = stmt.executeQuery(sb.toString());
				}

				d.next();

//				double circumBoth = d.getDouble(1);
				
//				double sharedCircumference = actPoly.getCircumference() + neighbourPoly.getCircumference() - circumBoth;
				double sharedCircumference=d.getDouble(1);
				
//				System.out.println("circumference actPoly: "+actPoly.getCircumference());
//				System.out.println("circumference NeighPoly: "+neighbourPoly.getCircumference());
//				System.out.println("circumference shared: "+sharedCircumference);
				
				actPoly.setCircumferenceshared(sharedCircumference);
			}
		}
		

		if (jdbc != null) {
			jdbc.close();
		}
	}
	
	//----------------Area Segmentation------------------------
	
	/**determines the ID of the geometry which contains the location
	 * @param PLZ5: boolean to indicate database
	 * @param numberlocations: number of used locations
	 * @throws SQLException
	 */
	public static void determineHomePoly(boolean PLZ5, int numberlocations,
			boolean microm) throws SQLException {

		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//init connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//init database table and columns
		String tablegeom = null;

		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		int id;
		StringBuffer sb = new StringBuffer();

		//getting geoemtry id for every location
		for (int i = 0; i < numberlocations; i++) {
			System.out.println(i+","+numberlocations);
			Location loc = locationContainer.getLocation(i);
			sb = new StringBuffer();
			
			// SELECT id FROM geometriesplz5 WHERE
			// ST_Contains(the_geom,st_setsrid(st_makepoint(13.72047,51.09358),4326))
			// LIMIT 1;
			
			//create SQL statement
			sb.append("SELECT id FROM " + tablegeom
					+ " WHERE ST_Contains(the_geom,ST_Setsrid(ST_Makepoint("
					+ loc.getLon() + "," + loc.getLat() + "),4326)) LIMIT 1;");
			ResultSet d = null;
			
			//execute Query and store id
			if (!microm) {
				d = stmt.executeQuery(sb.toString());
			}

			d.next();
			id = d.getInt(1);
			locationContainer.setHomePoly(i, id);
		}

		if (jdbc != null) {
			jdbc.close();
		}

	}
	
	/**initialise distances from every location to every geometry
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 * @throws SQLException
	 */
	private static void initDistances(int numberpolygons, int numberlocations,
			boolean microm) throws SQLException {
		
		polyDistances.distances = (ArrayList<Double>[]) new ArrayList[numberpolygons];
		for (int i = 0; i < polyDistances.distances.length; i++)
			polyDistances.distances[i] = new ArrayList<Double>();

		//calculate distance for every geometrie to every location
		for (int i = 0; i < numberpolygons; i++) {
			System.out.println(i);
			Polygon poly = polygonContainer.getPolygon(i);
			polyDistances.ids.add(poly.getId());

			for (int j = 0; j < numberlocations; j++) {
				double distance = -1;
				if (!microm) {
					distance = initDistancesToCentroid(j, poly);
				}

				polyDistances.distances[i].add(distance);
			}
		}
	}
	
	private static double initDistancesToCentroid(int location,
			Polygon actPoly) {
		
			Location loc = locationContainer.getLocation(location);
			
			double phi = Math.acos(Math.sin(loc.getLat())
						* Math.sin(actPoly.getCentroid()[1])
						+ Math.cos(loc.getLat())
						* Math.cos(actPoly.getCentroid()[1])
						* Math.cos(actPoly.getCentroid()[0]
								- loc.getLon()));
				double distance = phi * 6370;
			
			return distance;

	}
	
	
	/**allocates geomteries by their distance to the locations; every locations gets the nearest geometries
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 */
	private static void allocatePolygonsByDistance(int numberpolygons,
			int numberlocations) {

		//allocate every polygone to the location which is nearest
		for (int i = 0; i < numberpolygons; i++) {
			int locMinDist = 0;
			double minDistance = polyDistances.distances[i].get(0);
			for (int j = 1; j < numberlocations; j++) {
				if (polyDistances.distances[i].get(j) < minDistance) {
					locMinDist = j;
					minDistance = polyDistances.distances[i].get(j);
				}
			}

			int polyID = polyDistances.ids.get(i);
			System.out.println("write " + polyID + " to " + (locMinDist + 1));
			polygonContainer.setAllocatedLocation(i, locMinDist,
					locationContainer);

			System.out.println(i);
		}

	}

	/**initialise critvalues, necessary for rearranging
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 */
	private static void initCriteria(int numberpolygons, int numberlocations) {

		//reset criteria to 0 (necessary for whitespot)
		for (int i = 0; i < numberlocations; i++) {
			locationContainer.setCriteria(i, 0);
		}
		
		//sum critvalues
		for (int i = 0; i < numberpolygons; i++) {
			double crit = polygonContainer.getPolygon(i).getCriteria();
			Location loc = polygonContainer.getAllocatedLocation(i);
			double newcrit = loc.getCriteria() + crit;

			locationContainer.setCriteriaByLoc(loc, newcrit);
		}

		//print criteria values
		for (int i = 0; i < numberlocations; i++) {
			System.out.println(locationContainer.getCriteria(i));
		}
	}
	
	/**checks whether the created ares (created by allocatePolygonsByDistance) are coherent
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 * @throws Exception
	 */
	private static void checkUnityAfterAllocByDist(int numberpolygons, int numberlocations) throws Exception{
		//check unity of area for every location
		boolean areaNotCoherent =false;
		
		for (int j=0;j<numberlocations;j++){
			
			Location loc = locationContainer.getLocation(j);
			
			//init variables
			boolean unit;
			boolean first=true;
			int pos = 0;
			boolean graphEnds = false;
			
			//saving these neighbours, which can be reached from the actual poly AND are contained in buffAllocPolysLoc
			List<Integer> neighbours = new ArrayList<Integer>();
			
			//saving geometries which contain to graph
			List<Polygon> polysTaken = new ArrayList<Polygon>();
			
			//saving geomtries which are not rearranged jet, necessary during rearrangement of other graphs
			List<Polygon> polysNotTaken = new ArrayList<Polygon>();
			
			//saving all geometries which contain to that location
			List<Polygon> buffAllocPolysLoc = new ArrayList<Polygon>();
			
			//add all polygones which contain to the location
			for (int i = 0; i < numberpolygons; i++) {
				Polygon poly = polygonContainer.getPolygon(i);
				if (poly.getAllocatedLocation().getId() == loc.getId()) {
					buffAllocPolysLoc.add(poly);
				}
			}

			//check unity using graphs
			//approach: if not all polygones can be reached using relationsship of neighbours, the area is not coherent--> several graphs exist
			while (!graphEnds) {
				Polygon actPoly=null;
				
				//check whether it is first polygon: true, start with Homepoly to identify area that is nearest to location (if area is not coherent the other area will be rearranged)
				if (first){
					//identify homePoly and use it
					for (int i=0;i<buffAllocPolysLoc.size();i++){
						if (buffAllocPolysLoc.get(i).getId()==loc.getHomePolyId()){
							actPoly=buffAllocPolysLoc.get(i);
							neighbours.add(actPoly.getId());
							first=false;
						}
					}
				}
				else{
					//take next polygon in list
					actPoly = buffAllocPolysLoc.get(pos);
				}

					//check whether actPoly id is the next one in neighbour list; if so, take next neighbour
					boolean takeNextNeighbour = false;
					if (neighbours.size() > 0) {
						if (actPoly.getId() == neighbours.get(0)) {
							takeNextNeighbour = true;

						}
					} else {
						//necessary for start
						takeNextNeighbour = true;
					}

					if (takeNextNeighbour) {
						boolean allreadyTaken = false;
						//check whether the actual polygon is allready checked to contain in graph
						for (int i = 0; i < polysTaken.size(); i++) {
							if (actPoly.getId() == polysTaken.get(i).getId()) {
								allreadyTaken = true;
							}
						}

						//if it was not checked yet
						if (!allreadyTaken) {
							polysTaken.add(actPoly);

							//add all neighbours of actual poly which are contained in buffAllocPolysLoc AND can be reached from actual poly
							for (int l = 0; l < actPoly.getNeighbours().size(); l++) {
								for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
									
									//check whether neighbour is contained in buffAllocPolysLoc
									if (buffAllocPolysLoc.get(k).getId() == actPoly
											.getNeighbours().get(l).getId()) {

										//check whether id is allready stored in neighbours List
										if (!neighbours
												.contains(buffAllocPolysLoc
														.get(k).getId())) {
											neighbours.add(buffAllocPolysLoc
													.get(k).getId());

										}
									}
								}
							}
						}

						//take always first neighbour in list, if neighbour size > 0
						if (neighbours.size() > 0) {
							pos = 0;
							neighbours.remove(0);
						} else {
							//if size==0, no neighbour to check exists anymore
							graphEnds = true;
						}

					} else {
						pos++;
					}
			}

			int countPolysTaken = 0;

			//check whether all polygons of buffAllocPolysLoc were taken and found as neighbours; if not more than one graph exists
			for (int l = 0; l < polysTaken.size(); l++) {
				for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
					if (buffAllocPolysLoc.get(k).equals(polysTaken.get(l))) {
						countPolysTaken++;
					}
				}
			}

			if (buffAllocPolysLoc.size() == countPolysTaken) {
				unit = true;
			} else {
				unit = false;
			}

			//if area has more than one part, rearrange the other parts
			if (!unit){
				
				areaNotCoherent=true;
				
				//store all polygones which contain not to main graph but to other graph(s)
				for (int i=0;i<buffAllocPolysLoc.size();i++){
					if (!polysTaken.contains(buffAllocPolysLoc.get(i))){
						polysNotTaken.add(buffAllocPolysLoc.get(i));
					}
				}
				
				//take every poly which is aside and rearrange it to another area
				int posPoly=0;
				while(polysNotTaken.size()>0){
					
					//reset posPoly to 0 if necessary; reason: every polygon is taken and it is tried to rearrange it. Condition is, that it is need to
					//rearranged in that way that the resulting area is coherent. But if it is a geometry from the middle of the graph, this is not given
					//in every case, so just the others arround need to be rearranged and after this also the polygone in the middle can be rearranged to another
					//location. To achive this its necessary to go through the polysNotTaken once again
					if (posPoly>=polysNotTaken.size()){
						posPoly=0;
					}
					
					//take Polygon
					Polygon actPoly=polysNotTaken.get(posPoly);
					
					//init variables
					boolean rearranged=false;
					
					//store all ids from the locations where the geometry can not be rearranged because the resulting area would not be coherent
					List <Integer> notUseableLocs = new ArrayList<Integer>();
					notUseableLocs.add(loc.getId());
					
					while(!rearranged){
						double minDist =-1;
						Location locMinDist = null;
						
						int posActPoly = -1;
						for (int k=0;k<numberpolygons;k++){
							if (polyDistances.ids.get(k)==actPoly.getId()){
								posActPoly=k;
							}
						}
						
						boolean firstLoc=true;
						//determine Location with next smallest Dist
						for (int k=0;k<numberlocations;k++){
							//take location
							Location actLoc = locationContainer.getLocation(k);
							
							//check whether its the first one (to init minDist) and whether its not contained in notUseableLocs
							if (firstLoc && !notUseableLocs.contains(actLoc.getId())){
								//init variables
								minDist=polyDistances.distances[posActPoly].get(k);
								locMinDist=locationContainer.getLocation(k);
								firstLoc=false;
							}
							else{
								//check Distance and content of notUseableLocs
								if (polyDistances.distances[posActPoly].get(k)<minDist && !notUseableLocs.contains(actLoc.getId())){
									minDist=polyDistances.distances[posActPoly].get(k);
									locMinDist=locationContainer.getLocation(k);
								}
							}
						}
						
						
						//simulate Change to the location which is nearest to geometry
						locMinDist.getAllocatedPolygon().add(actPoly);
						actPoly.setAllocatedLocation(locMinDist);
						loc.removeAllocatedPolygon(actPoly);
	
						//check unity of area that gets the polygon
						boolean unity = checkUnitCalculationGets(actPoly.getId(), locMinDist.getId(), numberpolygons);
						
						
						//do or abort change
						if (unity){
							System.out.println("set "+actPoly.getId()+" from "+loc.getId()+" to "+locMinDist.getId());
							System.out.println("old: "+loc.getCriteria()+","+locMinDist.getCriteria());
							
							//change crit values
							double critPoly = actPoly.getCriteria();
							double critOld = loc.getCriteria();
							double critNew = critOld-critPoly;
							loc.setCriteria(critNew);
							
							critOld = locMinDist.getCriteria();
							critNew = critOld+critPoly;
							locMinDist.setCriteria(critNew);
							rearranged=true;
							polysNotTaken.remove(posPoly);
							
							System.out.println("new: "+loc.getCriteria()+","+locMinDist.getCriteria());
						}
						else{
							//abort if resulting area is not coherent--> reset change
							locMinDist.removeAllocatedPolygon(actPoly);
							actPoly.setAllocatedLocation(loc);
							loc.getAllocatedPolygon().add(actPoly);
							notUseableLocs.add(locMinDist.getId());
						}
						
						//check whether actual geometry can not be rearranged to one location, in this case the if statement is true
						if (notUseableLocs.size()==numberlocations){
							posPoly++;
							rearranged=true;
						}
					}
				}
			}
		}
		
		//check whether at last one area was not coherent
		if (areaNotCoherent){
			initCriteria(numberpolygons, numberlocations);
		}
	}

	/**checks whether criteria is balanced (threshold is reached) or rearrangement is necessary
	 * @param numberpolygons: number of geometries in calculation area
	 * @param numberlocations: number of used locations
	 * @param threshold: int value of threshold which variance between values is allowed
	 * @param PLZ5: boolean, indicates table
	 * @param weightCom: int, weighting value of compactness
	 * @param weightCrit: int, weighting value of criteria
	 * @param whitespot: boolean, whether whitespot or not
	 * @param numberGivenLocations: int, just necessary for whitespot
	 * @param numberNewLocations: int, just necessary for whitespot
	 * @throws SQLException
	 * @throws Exception
	 */
	public static void checkThreshold(int numberpolygons, int numberlocations,
			int threshold, boolean microm, boolean PLZ5,
			int weightCom, int weightCrit, boolean whitespot, int numberGivenLocations) throws SQLException, Exception {

		//init variables
		boolean satisfied = false;

		int run = 0;
		double critAverage = 0;

		double critSum = 0;

		//calculate average of criteria value; every location should have this value in best case
		for (int i = 0; i < numberlocations; i++) {
			critSum = critSum + locationContainer.getCriteria(i);
		}

		critAverage = critSum / numberlocations;

		//init compactness ratio for every area to make a comparison during rearranging possible
		for (int i = 0; i < numberlocations; i++) {
			initCompactnessRatio(locationContainer.getLocation(i), critAverage,
					numberpolygons, microm, PLZ5, weightCom, weightCrit);
		}

		int location = 0;

		//while variance given by threshold between the criteria values of the locations is not satisfied
		while (!satisfied) {
			
			//init variables
			int[] compCriterias = new int[numberlocations];
			int compCrits = 0;
			for (int i = 0; i < numberlocations; i++) {
				compCriterias[i] = 0;
				System.out.println(locationContainer.getLocation(i)
						.getCriteria());
			}

			//calculate difference from average value
			for (int i = 0; i < numberlocations; i++) {
				double value = locationContainer.getCriteria(i) * 100
						/ critAverage;
				double difference = -1;

				if (value > 100) {
					difference = value - 100;
				} else {
					difference = 100 - value;
				}

				//check whether difference is good; count number of locations which are balanced
				if (difference < (threshold)) {
					compCrits++;
				}
			}

//			int no = 0;
//			boolean arranged = false;
//
//				List<Integer> compCriteriasFail = new ArrayList<Integer>();

				//if not all locations are balanced, start rearranging
				if (compCrits != numberlocations) {

					System.out.println("location" + location);

					// check whether it is necessary to rearrange a polygon in
					// that area
					// get difference to average value
					
					
					double difference = calculateDifference(location, critAverage);

					System.out.println(difference + "," + critAverage);

					//check whether variance of critvalue is to big --> rearrange
					if (!nofoundlocations.contains((location + 1))
							&& difference > threshold) {

						rearrangePolys(numberpolygons,
								numberlocations, numberGivenLocations, (location + 1), critAverage,
								microm, PLZ5, weightCom, weightCrit, whitespot);
						
						 FileWriter output =createFileWriter();
						 writePolygon(output, numberpolygons);
						 output.close();
					}

					location++;
					//set location to 0 to start check again
					if (location >= numberlocations) {
						location = 0;
					}
				}
			run++;

			//if all critvalues are balanced rearranging will be stopped
			if (compCrits == numberlocations) {
				satisfied = true;
			}

			//break if too much runs for rearranging
			if (nofoundlocations.size() >= numberlocations || run > 500) {
				satisfied = true;
				System.out.println("Break");
			}

		}

		System.out.println("rearranged with a variance of " + threshold + "%");
		System.out.println("no better arrangement for " + nofoundlocations);
	}
	
	/**initialise the compactness ratio for comparison to change of compactness during rearranging
	 * @param loc: Location, actuall Location for which compactness ratio should be calculated
	 * @param critAverage: double, value of best criteria sum
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @param PLZ5: boolean; indicates table
	 * @param weightCom: int, weighting value for compactness
	 * @param weightCrit: int, weighting value for criteria
	 * @throws SQLException
	 */
	private static void initCompactnessRatio(Location loc, double critAverage,
			int numberpolygons, boolean microm, boolean PLZ5, int weightCom,
			int weightCrit) throws SQLException {
		
		//calculate weight and store it
		double weight = calculateWeightValue(loc, numberpolygons,
				microm, PLZ5, critAverage, weightCom, weightCrit);

		loc.setWeightValue(weight);
	}
	
	/**calculates weight value composite by compactness ratio and abberance to average criteria value 
	 * @param actLoc: Location, actual Location
	 * @param numberpolygons
	 * @param microm
	 * @param PLZ5
	 * @param critAverage
	 * @param weightCom
	 * @param weightCrit
	 * @return
	 * @throws SQLException
	 */
	public static double calculateWeightValue(Location actLoc,
			int numberpolygons, boolean microm, boolean PLZ5,
			double critAverage, int weightCom, int weightCrit)
			throws SQLException {
		
		//calculate Area and circumference
		double A_area = calculateAreaForWeight(numberpolygons, actLoc.getId());	
//		System.out.println("Area function: "+A_area);
//		double A_area = calculateAreaOrCircumference(actLoc.getId(), numberpolygons, microm, PLZ5, true);
//		System.out.println("Area database: "+A_area);
		
		double U_area = calculateCircumferenceForWeight(numberpolygons, actLoc.getId());
//		System.out.println("circumference function: "+U_area);
//		double U_area = calculateAreaOrCircumference(actLoc.getId(), numberpolygons, microm, PLZ5, false);
//		System.out.println("circumference function: "+U_area);

		// circumference circle r=U/(2*pi) --> A = pi*r^2 = pi*(U/2*pi)^2
		double A_circle = Math.PI * Math.pow((U_area / (2 * Math.PI)), 2);

		double ratioCom = A_area / A_circle;

		double compactness = 1 - ratioCom;

		// check criteria
		double ratioCrit = actLoc.getCriteria() / critAverage;
		double criteria = Math.abs(1 - ratioCrit);

		double weight = compactness * weightCom + criteria * weightCrit;

		return weight;
	}
	
	private static double calculateAreaForWeight(int numberpolygons, int loc){
		double area=0;
		
		for (int i=0;i<numberpolygons;i++){
			if (polygonContainer.getPolygon(i).getFlagAllocatedLocation()) {
				if (polygonContainer.getPolygon(i).getAllocatedLocation()
						.getId() == loc) {
					area=area+polygonContainer.getPolygon(i).getArea();
				}
			}
		}
		
		return area;
	}
	
	private static double calculateCircumferenceForWeight(int numberpolygons, int loc){
		double circum=0;
		
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getFlagAllocatedLocation()){
				if (actPoly.getAllocatedLocation().getId()==loc){
					circum=circum+actPoly.getCircumference();
					for (int j=0;j<actPoly.getNeighbours().size();j++){
						Polygon neighb = actPoly.getNeighbours().get(j);
						if (neighb.getFlagAllocatedLocation()){
							if (neighb.getAllocatedLocation().getId()==loc && actPoly.getId()!=neighb.getId()){
								circum=circum-actPoly.getCircumferenceShared(j);
							}
						}
					}
				}
			}
		}
		
		return circum;
	}
	
	/**calculates area or circumference of whole area for the location
	 * @param loc: int, location id for which the calculation should be done
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @param PLZ5: boolean, indicates table
	 * @param area: boolean, indicates whether area or circumference should be calcuated
	 * @return area value or circumference
	 * @throws SQLException
	 */
	private static double calculateAreaOrCircumference(int loc, int numberpolygons,
			boolean microm, boolean PLZ5, boolean area) throws SQLException {

		//init variables
		Connection jdbc = null;
		Statement stmt = null;

		//init connection
		if (!microm) {
			jdbc = getConnection();
			stmt = jdbc.createStatement();
		}

		//set table and columns
		String tablegeom = null;

		// PLZ5
		if (PLZ5) {
			tablegeom = "geometriesplz51";
		} else {
			// PLZ8
			tablegeom = "geometriesplz81";
		}

		//store all polygons that belong to the location
		StringBuffer sb = new StringBuffer();
		List<Integer> geomIDs = new ArrayList<Integer>();
		for (int i = 0; i < numberpolygons; i++) {
			if (polygonContainer.getPolygon(i).getFlagAllocatedLocation()) {
				if (polygonContainer.getPolygon(i).getAllocatedLocation()
						.getId() == loc) {
					geomIDs.add(polygonContainer.getPolygon(i).getId());
				}
			}
		}

		//formate String for SQL statement
		StringBuilder idsBuffer = new StringBuilder(geomIDs.toString());
		idsBuffer.deleteCharAt(0);
		idsBuffer.deleteCharAt(idsBuffer.length() - 1);

		//create SQL statement
		if (area){
			sb = calculateArea(tablegeom, idsBuffer);
		}
		else{
			sb = calculateCircumference(tablegeom, idsBuffer);
		}

		//execute query and store result
		ResultSet d = null;
		if (!microm) {
			d = stmt.executeQuery(sb.toString());
		}

		d.next();

		double result = d.getDouble(1);

		if (jdbc != null) {
			jdbc.close();
		}

		return result;

	}
	
	/**creates SQL statement for the calculation of the area
	 * @param tablegeom: string, name of table
	 * @param idsBuffer: StringBuilder, string of ids
	 * @return String for SQL statement
	 */
	private static StringBuffer calculateArea(String tablegeom, StringBuilder idsBuffer){
		StringBuffer sb = new StringBuffer();

		sb.append("SELECT ST_AREA(ST_UNION(the_geom)) FROM " + tablegeom
				+ " WHERE id IN (" + idsBuffer.toString() + ");");

		return sb;
	}
	
	/**creates SQL statement for the calculation of the circumference
	 * @param tablegeom: string, name of table
	 * @param idsBuffer: StringBuilder, string of ids
	 * @return String for SQL statement
	 */
	private static StringBuffer calculateCircumference(String tablegeom,StringBuilder idsBuffer) throws SQLException {
		StringBuffer sb = new StringBuffer();
		
		//SELECT ST_Length(ST_CollectionExtract(ST_Intersection(a_geom, b_geom), 2))
//		FROM (
//				  SELECT (SELECT the_geom from geometriesplz51 where id=8) AS a_geom,
//				  (SELECT the_geom from geometriesplz51 where id=29) AS b_geom
//				) f;
		
		sb.append("SELECT ST_PERIMETER(ST_UNION(the_geom)) FROM " + tablegeom
				+ " WHERE id IN (" + idsBuffer.toString() + ");");
		
		return sb;
	}
	
	private static double calculateDifference(int location, double critAverage){
		double value = locationContainer.getCriteria(location)
				* 100 / critAverage;
		double difference = -1;

		if (value > 100) {
			difference = value - 100;
		} else {
			difference = 100 - value;
		}
		
		return difference;
	}
	
	/**rearrange polygons dependent on best proportion of change of compactness and criteria
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @param numberlocations: int, number of used locations
	 * @param location: int, id of location
	 * @param critAverage: double, value to reach for criteria 
	 * @param PLZ5: boolean, to indicate databse
	 * @param weightCom: int, weight value of compactness
	 * @param weightCrit: int, weight value of criteria
	 * @throws Exception
	 */
	private static void rearrangePolys(int numberpolygons,
			int numberlocations, int numberGivenLocations, int location, double critAverage,
			boolean microm, boolean PLZ5, int weightCom, int weightCrit, boolean whitespot)
			throws Exception {

		// check whether area gives or gets a geometry
		boolean givesPoly = false;

		// locBasis = location that gets or gives a geometry
		Location locBasis = null;
		for (int i = 0; i < numberlocations; i++) {
			if (locationContainer.getLocation(i).getId() == location) {
				locBasis = locationContainer.getLocation(i);
			}
		}

		// create List of all polygones that belong to the location
		List<Polygon> rearrangePoly = new ArrayList<Polygon>();
		for (int i = 0; i < numberpolygons; i++) {
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getAllocatedLocation().getId() == location) {
				rearrangePoly.add(actPoly);
			}
		}

		// determine neighbour polygons
				List<Polygon> neighbourPolys = new ArrayList<Polygon>();

				// polygons of location
				for (int i = 0; i < rearrangePoly.size(); i++) {
					List<Polygon> neighbourIds = rearrangePoly.get(i).getNeighbours();

					// neighbours of one polygon
					for (int j = 0; j < neighbourIds.size(); j++) {

						boolean found = false;
						// compare every neighbour to List of polygones (rearrange
						// polys), whether it is containt in that list (found=true) or
						// not (found=false)
						for (int k = 0; k < rearrangePoly.size(); k++) {
							if (neighbourIds.get(j).getId() == rearrangePoly.get(k)
									.getId()) {
								found = true;
							}
						}

						if (!found) {
							if (!neighbourPolys.contains(neighbourIds.get(j))) {
								neighbourPolys.add(neighbourIds.get(j));
							}
						}
					}
				}
				// remove all neighbours which belong to an area that can't get new
				// polygones
				for (int i = 0; i < neighbourPolys.size(); i++) {
					boolean removed = false;

					for (int j = 0; j < nofoundlocations.size(); j++) {
						if (neighbourPolys.get(i).getAllocatedLocation().getId() == nofoundlocations
								.get(j)) {
							removed = true;
						}
					}

					if (removed) {
						neighbourPolys.remove(i);
						i--;
					}
				}

		//shrink the neighbours to neighbour areas which arent the homePolys
		List<Polygon> neighPolygonsNotHome = new ArrayList<Polygon>();
		for (int i = 0; i < neighbourPolys.size(); i++) {
			neighPolygonsNotHome.add(neighbourPolys.get(i));
		}

		if (!whitespot){
		for (int i = 0; i < numberlocations; i++) {
			for (int j = 0; j < neighbourPolys.size(); j++) {
				if (neighbourPolys.get(j).getId() == locationContainer
						.getLocation(i).getHomePolyId()) {

					for (int k = 0; k < neighPolygonsNotHome.size(); k++) {
						if (neighbourPolys.get(j).getId() == neighPolygonsNotHome
								.get(k).getId()) {
							neighPolygonsNotHome.remove(k);
						}
					}
				}
			}
		}
		}
		else{
			// check neighbourPolys whether they are hompolys, just for locations that are given
			for (int i = 0; i < numberlocations; i++) {
				for (int j = 0; j < neighbourPolys.size(); j++) {
					if (i<numberGivenLocations){
						if (neighbourPolys.get(j).getId() == locationContainer
								.getLocation(i).getHomePolyId()) {
		
							for (int k = 0; k < neighPolygonsNotHome.size(); k++) {
								if (neighbourPolys.get(j).getId() == neighPolygonsNotHome
										.get(k).getId()) {
									neighPolygonsNotHome.remove(k);
								}
							}
						}
					}
				}
			}
		}

		// create List of all neighbour Locations
				List<Location> neighbourLocations = new ArrayList<Location>();

				for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
					Location actLoc = neighPolygonsNotHome.get(i)
							.getAllocatedLocation();
					boolean contained = false;
					for (int j = 0; j < neighbourLocations.size(); j++) {
						if (neighbourLocations.get(j).getId() == actLoc.getId()) {
							contained = true;
						}
					}

					if (!contained && !nofoundlocations.contains(actLoc.getId())) {
						neighbourLocations.add(actLoc);
					}
				}

		System.out.println("unitsize:" + neighPolygonsNotHome.size());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			sb.append(neighPolygonsNotHome.get(i).getId() + ",");
		}
		System.out.println(sb);

		// check unity of polygons
		List<Polygon> neighPolygonsUnit = new ArrayList<Polygon>();
		for (int i = 0; i < neighPolygonsNotHome.size(); i++) {
			neighPolygonsUnit.add(neighPolygonsNotHome.get(i));
		}

//		for (int i = 0; i < neighbourLocations.size(); i++) {
//			givesPoly = false;
//
//			// determine whether area gives or gets an geometry
//			if (locBasis.getCriteria() > neighbourLocations.get(i)
//					.getCriteria()) {
//				givesPoly = true;
//			}
//
//			if (!givesPoly) {
//				for (int j = 0; j < neighPolygonsNotHome.size(); j++) {
//					boolean unit = false;
//
//					if (neighPolygonsNotHome.get(j).getAllocatedLocation()
//							.getId() == neighbourLocations.get(i).getId()) {
//						unit = FunctionsCommon.checkUnit(neighPolygonsNotHome.get(j).getId(),
//								neighPolygonsNotHome.get(j)
//										.getAllocatedLocation().getId(),
//								location, numberpolygons);
//
//						if (!unit) {
//							for (int k = 0; k < neighPolygonsUnit.size(); k++) {
//								if (neighPolygonsNotHome.get(j).getId() == neighPolygonsUnit
//										.get(k).getId()) {
//									neighPolygonsUnit.remove(k);
//								}
//							}
//
//						}
//					}
//
//				}
//			} else {
//				// remove all neighbours which belong to the location that gets
//				// a new geometry
//				for (int k = 0; k < neighPolygonsUnit.size(); k++) {
//					if (neighPolygonsUnit.get(k).getAllocatedLocation().getId() == neighbourLocations
//							.get(i).getId()) {
//						neighPolygonsUnit.remove(k);
//						k--;
//					}
//				}
//
//				// check all geometries of locBasis to determine all that can be
//				// given
//				for (int j = 0; j < rearrangePoly.size(); j++) {
//					boolean unit = false;
//
//					unit = FunctionsCommon.checkUnit(rearrangePoly.get(j).getId(), location,
//							neighbourLocations.get(i).getId(), numberpolygons);
//
//					// add all geometries that can be given
//					if (unit) {
//						if (!whitespot){
//							if (!neighPolygonsUnit.contains(rearrangePoly.get(j))
//									&& rearrangePoly.get(j).getId() != locBasis
//											.getHomePolyId()) {
//								neighPolygonsUnit.add(rearrangePoly.get(j));
//							}
//						}
//						else{
//							if (locBasis.getId()<numberGivenLocations){
//								if (!neighPolygonsUnit.contains(rearrangePoly.get(j))
//										&& rearrangePoly.get(j).getId() != locBasis
//												.getHomePolyId()) {
//									neighPolygonsUnit.add(rearrangePoly.get(j));
//								}
//							}
//							else{
//								if (!neighPolygonsUnit.contains(rearrangePoly.get(j))) {
//									neighPolygonsUnit.add(rearrangePoly.get(j));
//								}
//							}
//						}
//					}
//				}
//			}
//
//		}

		System.out.println("unitsize after:" + neighPolygonsUnit.size());
		sb = new StringBuffer();
		for (int i = 0; i < neighPolygonsUnit.size(); i++) {
			sb.append(neighPolygonsUnit.get(i).getId() + ",");
		}
		System.out.println(sb);

		if (neighPolygonsUnit.size() > 0) {

			double smallestChange = -1;
			int posSmallestChange = -1;
			givesPoly = false;
			Location locSmallestChange = null;

			// calculate compactness & detect polygon with best change (= best
			// ratio of change of criteria and compactness)
			for (int i = 0; i < neighPolygonsUnit.size(); i++) {
				Polygon actPoly = neighPolygonsUnit.get(i);

				// determine location of actPoly
				Location actLoc = neighPolygonsUnit.get(i)
						.getAllocatedLocation();

				// determine whether area gives or gets an geometry
				if (actLoc.getId() == locBasis.getId()) {
					givesPoly = true;
				}

				double changeValue = -1;

				//calculate change of compactness
				if (!givesPoly) {
					changeValue = FunctionsCommon.checkChangeofCompactness(actPoly, actLoc,
							location, critAverage, numberpolygons,
							numberlocations, microm, PLZ5, weightCom,
							weightCrit, givesPoly);

					if (i == 0) {
						smallestChange = changeValue;
						posSmallestChange = 0;
						locSmallestChange = actLoc;
					} else {
						if (changeValue < smallestChange) {
							smallestChange = changeValue;
							posSmallestChange = i;
							locSmallestChange = actLoc;
						}
					}
				} else {
					for (int j = 0; j < neighbourLocations.size(); j++) {
						actLoc = neighbourLocations.get(j);

						boolean unit = checkUnit(neighPolygonsUnit.get(i)
								.getId(), location, neighbourLocations.get(j)
								.getId(), numberpolygons);

						if (unit) {
							changeValue = checkChangeofCompactness(actPoly,
									actLoc, location, critAverage,
									numberpolygons, numberlocations, microm,
									PLZ5, weightCom, weightCrit, givesPoly);

							if (i == 0) {
								smallestChange = changeValue;
								posSmallestChange = 0;
								locSmallestChange = actLoc;
							} else {
								if (changeValue < smallestChange) {
									smallestChange = changeValue;
									posSmallestChange = i;
									locSmallestChange = actLoc;
								}
							}
						}
					}
				}
			}

			//get Polygon with best change of compactness
			Polygon polyToChange = neighPolygonsUnit.get(posSmallestChange);

			// determine location of polyToChange
			Location locChange = null;
			locChange = polyToChange.getAllocatedLocation();
			givesPoly = false;

			// determine whether area gives or gets an geometry
			if (locChange.getId() == locBasis.getId()) {
				givesPoly = true;
			}

			if (givesPoly) {
				locChange = locSmallestChange;
			}

			// rearrange Poly
			if (!givesPoly) {
				locBasis.getAllocatedPolygon().add(polyToChange);
				polyToChange.setAllocatedLocation(locBasis);

				for (int i = 0; i < locChange.getAllocatedPolygon().size(); i++) {
					if (locChange.getAllocatedPolygon().get(i).getId() == polyToChange
							.getId()) {
						locChange.getAllocatedPolygon().remove(i);
					}
				}

				changeCriteriaAfterRearrange(polyToChange.getId(),
						locBasis.getId(), locChange.getId(), numberpolygons);
			} else {
				locChange.getAllocatedPolygon().add(polyToChange);
				polyToChange.setAllocatedLocation(locChange);

				for (int i = 0; i < locBasis.getAllocatedPolygon().size(); i++) {
					if (locBasis.getAllocatedPolygon().get(i).getId() == polyToChange
							.getId()) {
						locBasis.getAllocatedPolygon().remove(i);
					}
				}

				changeCriteriaAfterRearrange(polyToChange.getId(),
						locChange.getId(), locBasis.getId(), numberpolygons);
			}

			if (location == 10 || location == 8) {
				StringBuffer debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == location) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locBasis after:" + debugging);

				debugging = new StringBuffer();
				for (int i = 0; i < numberpolygons; i++) {
					Polygon actPoly = polygonContainer.getPolygon(i);
					if (actPoly.getAllocatedLocation().getId() == locChange
							.getId()) {
						debugging.append(actPoly.getId() + ",");
					}
				}

				System.out.println("locChange after" + debugging);
			}
		} else {
			if (!nofoundlocations.contains(locBasis.getId())) {
				nofoundlocations.add(locBasis.getId());
				System.out.println("nofoundlocation added " + locBasis.getId());
			}
		}

	}

	/**calculates change of compactness and criteria using weighted function
	 * @param actPoly: Polygon, geometrie for which th calculation should be done
	 * @param actLoc: Location, location where the geometrie belongs to
	 * @param location: int, location id
	 * @param critAverage: double, value to reach for criteria  
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @param numberlocations: int, number of used locations
	 * @param PLZ5: boolean, to indicate the database
	 * @param weightCom: double, weight value for compactness
	 * @param weightCrit: double, weighting value for criteria
	 * @param givesPoly: boolean, indicates whether area of actLoc gives or gets a geometry
	 * @return
	 * @throws SQLException
	 */
	public static double checkChangeofCompactness(Polygon actPoly,
			Location actLoc, int location, double critAverage,
			int numberpolygons, int numberlocations, boolean microm,
			boolean PLZ5, int weightCom, int weightCrit, boolean givesPoly)
			throws SQLException {

		double rateCompCritGive = -1;
		double rateCompCritGet = -1;

		int[] locationIDS = new int[2];
		locationIDS[0] = location;
		locationIDS[1] = actLoc.getId();

		// location Basis= location thats gives or gets a geometry
		Location locBasis = null;

		for (int i = 0; i < numberlocations; i++) {
			if (locationContainer.getLocation(i).getId() == location) {
				locBasis = locationContainer.getLocation(i);
			}
		}

		// simulate Change
		if (givesPoly) {
			actLoc.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(actLoc);

			for (int i = 0; i < locBasis.getAllocatedPolygon().size(); i++) {
				if (locBasis.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					locBasis.getAllocatedPolygon().remove(i);
				}
			}
		} else {
			locBasis.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(locBasis);

			for (int i = 0; i < actLoc.getAllocatedPolygon().size(); i++) {
				if (actLoc.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					actLoc.getAllocatedPolygon().remove(i);
				}
			}
		}

		// do calculation for location that gets the geometry and that gives the
		// geometry
		// taken compactness algorithm: Cox algorithm; ratio of an area of a
		// geometry to the area of a circle with same circumference
		// compactness value should be as closed as possible to 1
		for (int i = 0; i < 2; i++) {
			Location loc =null;
				loc = locationContainer.getLocationByID(locationIDS[i]);
			double weight = calculateWeightValue(loc,
					numberpolygons, microm, PLZ5, critAverage, weightCom,
					weightCrit);

			if (i == 0) {
				rateCompCritGive = weight;
			} else {
				rateCompCritGet = weight;
			}
		}

		// check change of weightedValue ; value >0 compactness getting worse,
		// value<0 compactness getting better
		double changeGive = rateCompCritGive - locBasis.getWeightValue();
		double changeGet = rateCompCritGet - actLoc.getWeightValue();

		// reset change
		if (givesPoly) {
			locBasis.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(locBasis);

			for (int i = 0; i < actLoc.getAllocatedPolygon().size(); i++) {
				if (actLoc.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					actLoc.getAllocatedPolygon().remove(i);
				}
			}
		} else {
			actLoc.getAllocatedPolygon().add(actPoly);
			actPoly.setAllocatedLocation(actLoc);

			for (int i = 0; i < locBasis.getAllocatedPolygon().size(); i++) {
				if (locBasis.getAllocatedPolygon().get(i).getId() == actPoly
						.getId()) {
					locBasis.getAllocatedPolygon().remove(i);
				}
			}
		}

		// sum could be < 0 if both compactness getting better or one is getting
		// better much
		return (changeGive + changeGet);
	}
	
	/**checks whether both areas are coherent if a geometry will be rearranged
	 * @param polyID: int, id of geometry that should be rearranged
	 * @param locGive: int, id of location that gives the geometry
	 * @param locGet: int, id of location that gets the geometry
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @return boolean, true if both areas are coherent after rearrangement
	 * @throws InterruptedException
	 */
	public static boolean checkUnit(int polyID, int locGive, int locGet,
			int numberpolygons) throws InterruptedException {
		boolean unit = false;

		int numberpolysLocGive=0;
		
		//count number of polygons that belong to the location that gives geometry
		//necessary because it is not allowed to give geometry away, if just 1 polygon exists
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly = polygonContainer.getPolygon(i);
			if (actPoly.getAllocatedLocation().getId()==locGive){
				numberpolysLocGive++;
			}
		}
		
		boolean unitGives=false;
		boolean unitGets=false;
		
		if (numberpolysLocGive>1){
		//check unity of both areas
			unitGives = checkUnitCalculationGives(polyID, locGive,
				numberpolygons);
			unitGets = checkUnitCalculationGets(polyID, locGet,
				numberpolygons);
		}

		if (unitGets && unitGives) {
			unit = true;
		}

		return unit;
	}
	
	/**checks coherence of that area that gives the geometry
	 * @param polyID: int, id of geometry that should be rearranged
	 * @param loc: int, id of location that gives the geometry
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @return: boolean, true if resulting area is coherent
	 * @throws InterruptedException
	 */
	private static boolean checkUnitCalculationGives(int polyID, int loc,
			int numberpolygons) throws InterruptedException {
		boolean unit = false;

		// check unit of location that gives geometry
		// check unit by using graphs
		//idea: if there exist at least two graphs the area is NOT coherent
		//therefore checking whether all geometries can be reached
		
		//init variables
			int pos = 0;
			boolean graphEnds = false;
			List<Integer> neighbours = new ArrayList<Integer>();
			List<Polygon> polysTaken = new ArrayList<Polygon>();
			List<Polygon> buffAllocPolysLoc = new ArrayList<Polygon>();
			for (int i = 0; i < numberpolygons; i++) {
				Polygon poly = polygonContainer.getPolygon(i);
				if (poly.getAllocatedLocation().getId() == loc) {
					buffAllocPolysLoc.add(poly);
				}
			}

			for (int i = 0; i < buffAllocPolysLoc.size(); i++) {
				if (buffAllocPolysLoc.get(i).getId() == polyID) {
					buffAllocPolysLoc.remove(i);
				}
			}

			//while no end of graph is found
			while (!graphEnds) {
				//take polygon
				Polygon actPoly = polygonContainer.getPolygon(pos);

				if (actPoly.getAllocatedLocation().getId() == loc
						&& actPoly.getId() != polyID) {

					boolean takeNextNeighbour = false;
					if (neighbours.size() > 0) {
						if (actPoly.getId() == neighbours.get(0)) {
							takeNextNeighbour = true;

						}
					} else {
						takeNextNeighbour = true;
					}

					if (takeNextNeighbour) {
						boolean allreadyTaken = false;
						for (int i = 0; i < polysTaken.size(); i++) {
							if (actPoly.getId() == polysTaken.get(i).getId()) {
								allreadyTaken = true;
							}
						}

						if (!allreadyTaken) {
							polysTaken.add(actPoly);

							for (int j = 0; j < actPoly.getNeighbours().size(); j++) {
								for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
									if (buffAllocPolysLoc.get(k).getId() == actPoly
											.getNeighbours().get(j).getId()) {

										if (!neighbours
												.contains(buffAllocPolysLoc
														.get(k).getId())) {
											neighbours.add(buffAllocPolysLoc
													.get(k).getId());

										}
									}
								}
							}
						}

						if (neighbours.size() > 0) {
							if (neighbours.get(0)==actPoly.getId()){
								neighbours.remove(0);
							}
							pos = 0;
						} else {
							graphEnds = true;
						}

					} else {
						pos++;
					}
				} else {
					pos++;
				}
			}

			int countPolysTaken = 0;

			for (int j = 0; j < polysTaken.size(); j++) {
				for (int k = 0; k < buffAllocPolysLoc.size(); k++) {
					if (buffAllocPolysLoc.get(k).equals(polysTaken.get(j))) {
						countPolysTaken++;
					}
				}
			}

			if (buffAllocPolysLoc.size() == countPolysTaken) {
				unit = true;
			} else {
				unit = false;
			}

		return unit;
	}
	
	/**checks coherence of that area that gets the geometry
	 * @param polyID: int, geometry that will be rearranged
	 * @param loc: int, id of location that gets the geometry
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @return boolean, true if resulting area is coherent
	 */
	public static boolean checkUnitCalculationGets(int polyID, int loc,
			int numberpolygons) {
		boolean unit = false;

		// check unit of location that gets geometry

		// get Position of poly

		Polygon poly = polygonContainer.getPolygonById(numberpolygons, polyID);
		boolean foundNeighbour = false;
		int counter = 0;

		// check neighbours, if every geometry have a neighbour the area is coherent
		while (!foundNeighbour) {
			// take all neighbours of poly
			for (int j = 0; j < poly.getNeighbours().size(); j++) {
				// take all polys of location
				if (poly.getNeighbours().get(j).getId()!=poly.getId()){
					for (int k = 0; k < numberpolygons; k++) {
						Polygon actPoly = polygonContainer.getPolygon(k);
						if (actPoly.getFlagAllocatedLocation()) {
							if (actPoly.getAllocatedLocation().getId() == loc) {
								if (poly.getNeighbours().get(j).getId() == actPoly
										.getId()) {
									unit = true;
									foundNeighbour = true;
								}
							}
						}
					}
				}
				counter++;
			}

			if (counter == poly.getNeighbours().size() && !foundNeighbour) {
				foundNeighbour = true;
			}
		}

		return unit;
	}

	/**changes the criteria values of the two location that rearranged a geometry
	 * @param polyID: int, id of geometry that was rearranged
	 * @param location: int, id of location that gets geometry
	 * @param locationMaxCriteria: int, id of location that gives geometry
	 * @param numberpolygons: int, number of geometries in calculation area
	 * @throws SQLException
	 */
	public static void changeCriteriaAfterRearrange(int polyID, int location,
			int locationMaxCriteria, int numberpolygons) throws SQLException {
		// get criteria of the given polygon

		Polygon poly = polygonContainer.getPolygonById(numberpolygons, polyID);
		Location locMaxCrit = locationContainer
				.getLocationByID(locationMaxCriteria);
		Location loc = locationContainer.getLocationByID(location);

		double critValue = poly.getCriteria();

		System.out.println("criterias before:" + loc.getCriteria() + ","
				+ locMaxCrit.getCriteria());
		
		//calculate new critValues
		double locMaxCritValue = locMaxCrit.getCriteria() - critValue;
		double locCritValue = loc.getCriteria() + critValue;

		//store new critValues
		locationContainer.setCriteriaByLoc(locMaxCrit, locMaxCritValue);
		locationContainer.setCriteriaByLoc(loc, locCritValue);
		
		
		System.out.println("criterias after:" + loc.getCriteria() + ","
				+ locMaxCrit.getCriteria());
		System.out.println("reaarange polygon " + polyID
				+ " with a criteria of " + critValue + " from "
				+ locationMaxCriteria + " to " + location);
	}

	
	//----------------Visualisation------------------------

	// * Write Polygons into shape, just necessary for testing purposes
		// * @param buffershp: name of the ShapeBuffer
		// * @param shpWriter: name of the ShapeWriter
		// * @param bufferPoly: IDs of Polygons which belong/are allocated to the
		// location
		// * @param geomPoly: Geometries of Polygons which belong/are allocated to
		// the location
		// * @param location
		// * @throws Exception
		// */
		public static void writePolygon(FileWriter output, int numberpolygons)
				throws Exception {
			for (int i = 0; i < numberpolygons; i++) {
				Polygon poly = polygonContainer.getPolygon(i);
				Location loc = polygonContainer.getAllocatedLocation(i);

				if (loc != null) {
					output.append(Objects.toString(poly.getId()));
					output.append(";");
					output.append(Objects.toString(loc.getId()));
					output.append("\n");
				}

			}
		}
		
		/**visualize distribution of criteria for every location
		 * @param numberlocations: int, number of used locations
		 */
		public static void showCritResult(int numberlocations) {
			for (int i = 0; i < numberlocations; i++) {
				System.out.println("Critsize location " + i + " :"
						+ locationContainer.getCriteria(i));
			}
		}
		
	//----------------common functions------------------------
		public static FileWriter createFileWriter() throws IOException{
			FileWriter output = createFileWriter("polygones");
			
			return output;
		}
		
	/**Creates FileWriter for saving the results
	 * @return FileWriter
	 * @throws IOException
	 */
	public static FileWriter createFileWriter(String name) throws IOException { 
		String filename = name + System.currentTimeMillis() + ".csv";
		FileWriter output = new FileWriter(filename);
		output.append(new String("ID,Location"));
		output.append("\n");

		return output;
	}

	/**creates FileWriter for locations to visualize them
	 * @param numberlocations: int, number of given locations
	 * @throws IOException
	 */
	public static void createFileWriterLocs(int numberlocations)
			throws IOException {
		FileWriter outputloc = new FileWriter("locations.csv");
		outputloc.append(new String("ID, Long, Lat"));
		outputloc.append("\n");

		int coordpos = 0;
		for (int i = 1; i < (numberlocations + 1); i++) {
			Location loc = locationContainer.getLocation(i - 1);
			outputloc.append(Objects.toString(i));
			outputloc.append(",");
			outputloc.append(Objects.toString(loc.getLon()));
			outputloc.append(",");
			outputloc.append(Objects.toString(loc.getLat()));
			outputloc.append("\n");
			coordpos = coordpos + 2;
		}
		outputloc.close();
	}

	
	/**creates connection to database
	 * @return Connection
	 */
	public static Connection getConnection() {
		Connection jdbc = null;
		try {
			Class.forName("org.postgresql.Driver");
			jdbc = DriverManager.getConnection(
					"jdbc:postgresql://localhost:5432/MA", "postgres", "");
			System.out.println("Opened database successfully");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}

		return jdbc;

	}


	/**initialises the LocationContainer for storing the given locations
	 * @return LocationContainer
	 */
	public static LocationContainer initLocationContainer() {
		locationContainer = new LocationContainer();
		return locationContainer;
	}
	
	
	/**sets the given locations, parsing them from a example file
	 * @param numberlocations: int, number of given lcoations
	 * @param microm: boolean, indicates database
	 * @throws IOException
	 */
	public static void setLocations(int numberlocations, boolean microm)
			throws IOException {
		locationContainer = initLocationContainer();

		// Input file which needs to be parsed
		String fileToParse = null;
		if (!microm) {
			fileToParse = "E:\\Studium\\Master\\4.Semester - MA\\OSD_Standorte_MC.csv";
		} else {
			fileToParse = "C:\\Users\\s.schmidt@microm-mapchart.com\\Desktop\\Praktikum\\MA\\OSD_Standorte_MC.csv";
		}
		BufferedReader fileReader = null;

		// Delimiter used in CSV file
		final String DELIMITER = ";";
		int pos = 0;

		boolean satisfied = false;
		int i = 0;
		List<Integer> ids = new ArrayList<Integer>();

		// locations small area: 3, 5, 11, 12, 14, 21, 26, 33, 42, 53, 72
		// locations huge area: 11, 41, 55, 68, 72, 79, 82, 90, 92, 96
		ids.add(3); // DD Goldener Reiter
		ids.add(5); // DD Weixdorf
		// ids.add(10); //DD Elbcenter
		ids.add(11); // DD Wilder Mann
		ids.add(12); // DD Cossebaude
		ids.add(14); // DD Löbtau
		ids.add(21); // DD Leubnitz
		ids.add(26); // DD Leuben
		// ids.add(29); //DD Seidnitz
		ids.add(33); // DD Johannstadt
		// ids.add(34); //DD Sparkassenhaus
		// ids.add(39); //DD Weißig
		// ids.add(41); //Radeberg Hauptstra�e
		ids.add(42); // Radeberg
		// ids.add(51); //Kesselsdorf
		ids.add(53); // Possendorf
		// ids.add(54); //Kreischa
		// ids.add(55); //Rabenau
		// ids.add(56); //Tharandt
		// ids.add(60); //Altenberg
		// ids.add(68); //Struppen
		ids.add(72); // DD Heidenau West
		// ids.add(77); //Bergießhübel
		// ids.add(79); //Liebstadt
		// ids.add(82); //Neustadt
		// ids.add(90); //Panschwitz Kuckau
		// ids.add(92); //Schwepnitz
		// ids.add(96); //Hoyerswerda Altstadt

		String line = "";
		// Create the file reader
		fileReader = new BufferedReader(new FileReader(fileToParse));
		line = fileReader.readLine();

		while (!satisfied) {
			line = fileReader.readLine();

			if (line == null) {
				satisfied = true;
			} else {
				// Get all tokens available in line
				String[] tokens = line.split(DELIMITER);

				if (ids.contains(Integer.valueOf(tokens[0]))) {
					i++;
					double lon = Double.parseDouble(tokens[7]);
					double lat = Double.parseDouble(tokens[8]);
					locationContainer.add(i, lon, lat);
					pos = pos + 2;
				}

				if (i == numberlocations) {
					satisfied = true;
				}
			}
		}

		fileReader.close();
	}

	// --------------------------------------------------------------------
	// functions for Greenfield & Whitespot
	// --------------------------------------------------------------------

	
	
	// --------------------------------------------------------------------
	// functions for Whitespot
	// --------------------------------------------------------------------
	
	
}
