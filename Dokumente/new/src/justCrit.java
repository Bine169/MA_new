package MA;

import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;

import com.mapchart.common.geo.file.shape.ShapeBuffer;
import com.mapchart.common.geo.file.shape.ShapeType;
import com.mapchart.common.geo.file.shape.ShapeWriter;
import com.mapchart.common.geom.buffer.DoubleBuffer;
import com.mapchart.common.io.jdbc.JDBCConnection;
import com.mapchart.common.io.jdbc.JDBCManager;
import com.mapchart.common.io.jdbc.JDBCTypes;
import com.mapchart.core.routing.server.Wkttests;
import com.mapchart.core.server.config.ServerGlobal;

public class justCrit {
	
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
	private static void allocatePolygons(int numberlocations, int numberpolygons, double[] criteria, boolean PLZ5) throws Exception{
		JDBCConnection jdbc = functions.getConnectionMicrom();
		String columnIDs=null;
		//PLZ5
		if (PLZ5){
			columnIDs="_g7304";
		}
		else{
		//PLZ8
			columnIDs="_g7305";
		}
		
		//get all PolygonIDs and store it
		StringBuffer sb = new StringBuffer();
		//SELECT t2.id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM _varea_1424340553765 AS t1 INNER JOIN _vcriteria_1424340553765 As t2 ON t2._g7304=t1.id
		sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM geometries AS t1 INNER JOIN criterias AS t2 ON t2."+columnIDs+"=t1.id");
		System.out.println(sb);
		ResultSet t=jdbc.executeQuery(sb.toString());
			
		for (int i=0;i<numberpolygons;i++){
			t.next();
			polys[0].add(t.getDouble("id"));
			polysGeometry[0].add(t.getString("id"));
			polysGeometry[1].add(t.getString("the_geom"));
			polysGeometry[2].add(t.getString("criteria"));
		}
			
		System.out.println("length"+polys[0].size());
			
		
		while (polys[0].size()!=0){
			
			double minCriteria=criteria[0];
			int locationMinCriteria=1;
			
			for (int j=1;j<criteria.length;j++){
				if (criteria[j]<minCriteria){
					minCriteria=criteria[j];
					locationMinCriteria=j+1;
				}
			}
			
			int polyID = polys[0].get(0).intValue();
	
			double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
			
//			for (int j=1;j<polys[0].size();j++){
//				double critValueact = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polys[0].get(j).intValue()))));
//				if (critValueact<critValue){
//					polyID = polys[0].get(j).intValue();
//					critValue=Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(Integer.toString(polyID))));
//				}
//			}
			
			System.out.println("write "+polyID+" to "+(locationMinCriteria));
			allocPolys[locationMinCriteria-1].add(polyID);
			String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
			geomAllocPolys[locationMinCriteria-1].add(geometry);
			
			addToCriteria(polyID, locationMinCriteria, criteria);
			polys[0].remove(Double.valueOf(polyID));
		}
		
	}
	
	public static void main(String[] args)
	throws Exception {

		boolean PLZ5=false;
		boolean common=true;
		functions.initConnectionMicrom(PLZ5, common);
		
		long time = System.currentTimeMillis();
		
		//setLocations
		int numberlocations;
		if (common){
				numberlocations =10;
			}
			else{
				numberlocations =55;
			}
		lonlats= new double[numberlocations*2];
		lonlats=functions.setLocations(numberlocations, PLZ5, common);
		
		//create FileWriter
//		FileWriter output = functions.createFileWriter();
		functions.createShapeWriter();
//		functions.createFileWriterLocs(numberlocations, lonlats);
		functions.writeLocationsShape(numberlocations, lonlats);
		
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
		int numberpolygons=functions.getNrOrSum(true, PLZ5);
		
//		//alocate Polygons to locations
		allocatePolygons(numberlocations, numberpolygons, criteria, PLZ5);
		
//		//Create Shapefile with allocated poylgons
		for (int i=0; i<numberlocations;i++){
//			functions.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
			functions.writePolygonShape(allocPolys[i], geomAllocPolys[i],i+1,criteria);
		}
		
		functions.closeShape();
		
//		StringBuffer sb = new StringBuffer();
//		for (int i=0;i<numberlocations;i++){
//			sb.append(i+":"+criteria[i]+";");
//			if (i==20 || i==40){
//				System.out.println(sb);
//				sb= new StringBuffer();
//			}
//		}
//		System.out.println(sb);
//		System.out.println("1:"+criteria[0]+",2:"+criteria[1]);
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");
//		
//		output.flush();
//		output.close();
	    
	    System.out.println("successfully ended");
}
}
