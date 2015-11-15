import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;

public class sumDivNumberDist {
	
	private static double lonlats[]; //stores locations
	private static ArrayList<Double>[] polys; //stores ID and distances to location
	private static ArrayList<String>[] polysGeometry; //stores ID and geometry of polys
	private static ArrayList<Integer>[] allocPolys; //stores allocated polys dependent on location 
	private static ArrayList<String>[] geomAllocPolys; //stores geometries of allocated polygons

//	
//	/**
//	 * Add value of polygon to criteria
//	 * @param polyID: ID of Polygon
//	 * @param location
//	 * @param jdbc: JDBCConnection
//	 * @param criteria: Array of all criterias
//	 * @throws SQLException
//	 */
	private static void addToCriteria(int polyID, int location, double[] criteria) throws SQLException{
		//get criteria of the given polygon
		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
		
		
			criteria[location-1]=criteria[location-1]+critValue;
		
	}
//	
//	/**
//	 * Assign polygons which are near to the locations
//	 * @param numberpolygons: number of all polygons of the region
//	 * @param criteria: array of criteria sum for distribute polygons homogeneously
//	 * @throws Exception
//	 */
	private static void allocatePolygons(int numberlocations, int numberpolygons, double[] criteria, int sumcriteria) throws Exception{
		Statement stmt = functions.getConnection();
		String columnIDs="_g7304";
			
		//get all PolygonIDs and store it
		StringBuffer sb = new StringBuffer();
		//SELECT t2.id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM _varea_1424340553765 AS t1 INNER JOIN _vcriteria_1424340553765 As t2 ON t2._g7304=t1.id
		sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM geometries AS t1 INNER JOIN criterias AS t2 ON t2."+columnIDs+"=t1.\"ID\"");
		System.out.println(sb);
		ResultSet t=stmt.executeQuery(sb.toString());
			
		for (int i=0;i<numberpolygons;i++){
			t.next();
			polys[0].add(t.getDouble("id"));
			polysGeometry[0].add(t.getString("id"));
			polysGeometry[1].add(t.getString("the_geom"));
			polysGeometry[2].add(t.getString("criteria"));
		}
			
		System.out.println("length"+polys[0].size());
		double totalsum=sumcriteria/numberlocations;
		double distances[] = new double[numberlocations];
		
		for (int i=0; i<polys[0].size();i++){
			int poscoords=0;
			String geometry = polysGeometry[1].get(i);
			for (int j=1; j<numberlocations+1;j++){
				polys[j].add(functions.calculateDistance(poscoords, geometry, stmt, lonlats));
				poscoords=poscoords+2;
			}
		}
		
		for (int i=0;i<numberlocations;i++){
			double sumloc=0;
			while (sumloc<totalsum && polys[0].size()>0){
				
				double minDistance = polys[i+1].get(0);
				for (int j=1;j<polys[i].size();j++){
					if (polys[i+1].get(j)<minDistance){
						minDistance=polys[i+1].get(j);
					}
				}
				
				int polyID = polys[0].get(polys[i+1].indexOf(minDistance)).intValue();
				double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
				sumloc=sumloc+critValue;
				
				System.out.println("write "+polyID+" to "+(i+1));
				allocPolys[i].add(polyID);
				String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
				geomAllocPolys[i].add(geometry);
				
				addToCriteria(polyID, i+1, criteria);
				for (int j=1;j<numberlocations+1;j++){
					polys[j].remove(polys[0].indexOf((double)polyID));
				}
				polys[0].remove(Double.valueOf(polyID));
			}
		}
	}
	
	public static void main(String[] args)
	throws Exception {

		long time = System.currentTimeMillis();
		
		//setLocations
		int numberlocations =2;
		lonlats= new double[numberlocations*2];
		lonlats=functions.setLocations(numberlocations);
		
		//create FileWriter
		FileWriter output = functions.createFileWriter();
		functions.createFileWriterLocs(numberlocations, lonlats);
		
		//initialize variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		polys = (ArrayList<Double>[])new ArrayList[numberlocations+1];
		for(int i=0;i<polys.length;i++) polys[i] = new ArrayList<Double>();
		
		polysGeometry = (ArrayList<String>[])new ArrayList[3];
		for(int i=0;i<polysGeometry.length;i++) polysGeometry[i] = new ArrayList<String>();
		
		allocPolys = (ArrayList<Integer>[])new ArrayList[numberlocations];
		for(int i=0;i<allocPolys.length;i++) allocPolys[i] = new ArrayList<Integer>();
		
		geomAllocPolys = (ArrayList<String>[])new ArrayList[numberlocations];
		for(int i=0;i<geomAllocPolys.length;i++) geomAllocPolys[i] = new ArrayList<String>();

		
//		//calculate number of Polygons in that region
		int numberpolygons=functions.getNrOrSum(true);
		int sumcriteria=functions.getNrOrSum(false);
		
//		//alocate Polygons to locations
		allocatePolygons(numberlocations, numberpolygons, criteria, sumcriteria);
		
//		//Create Shapefile with allocated poylgons
		for (int i=0; i<numberlocations;i++){
			functions.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
		}
//		System.out.println("1:"+criteria[0]+",2:"+criteria[1]+"3:"+criteria[2]+"4:"+criteria[3]);
		System.out.println("1:"+criteria[0]+",2:"+criteria[1]);
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");
//		
		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
}
}
