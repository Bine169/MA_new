package MA;

import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;

import com.mapchart.common.io.jdbc.JDBCConnection;

public class CritDistInOutTrueNearest {
	
	private static double lonlats[]; //stores locations
	private static ArrayList<Double>[] polys; //stores ID and distances to location
	private static ArrayList<Double>[] bufferpolys; //stores ID and distances to location
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
//	public static void addToCriteria(int polyID, int location, int locationMaxCriteria, double[] criteria, boolean rearranged) throws SQLException{
//		//get criteria of the given polygon
//		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(String.valueOf(polyID))));
//		
//		if (!rearranged){
//			criteria[location-1]=criteria[location-1]+critValue;
//		}
//		else{			
//			System.out.println("criterias before: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
//			criteria[locationMaxCriteria-1]=criteria[locationMaxCriteria-1]-critValue;
//			criteria[location-1]=criteria[location-1]+critValue;
//			System.out.println("criterias after: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
//		}
//		
//	}
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
		double distances[] = new double[numberlocations];
		
		for (int i=0;i<numberpolygons;i++){
			t.next();
			polys[0].add(t.getDouble("id"));
			bufferpolys[0].add(t.getDouble("id"));
			polysGeometry[0].add(t.getString("id"));
			polysGeometry[1].add(t.getString("the_geom"));
			polysGeometry[2].add(t.getString("criteria"));
		}
		
		for (int i=0;i<polys[0].size();i++){
			String geometry = polysGeometry[1].get(i);
			int poscoords=0;
			for (int j=1; j<numberlocations+1;j++){
				distances[j - 1] = functions.calculateDistance(poscoords, geometry, jdbc, lonlats);
				polys[j].add(distances[j-1]);
				bufferpolys[j].add(distances[j-1]);
				poscoords=poscoords+2;
			}
			
			System.out.println(i);
		}
			
		System.out.println("length"+polys[0].size());
		int lastID=-1;
		int lastPolyID=-1;
		
		while (polys[0].size()>0){
//		for (int i=0;i<numberlocations;i++){
//			System.out.println(criteria[i]);
//		}
//			System.out.println("poly "+i);
			double minCriteria=criteria[0];
			int locationMinCriteria=1;
			
			//get location with smallest Crit
			for (int j=1;j<criteria.length;j++){
				if (criteria[j]<minCriteria){
					minCriteria=criteria[j];
					locationMinCriteria=j+1;
				}
			}	
			
			//get nearest Poly to location with smallest Crit
			int locMinDist = 0;
			double minDistance = polys[locationMinCriteria].get(0);
			for (int j=1;j<polys[locationMinCriteria].size();j++){
				double actdist=polys[locationMinCriteria].get(j);
				if (actdist<minDistance){
					locMinDist=j;
					minDistance=actdist;
				}
			}
				
			int polyID = polys[0].get(locMinDist).intValue();
			
			boolean nearer=false;
			//check whether Poly is nearer to another one
			int locNearer=-1;
			for (int j=1;j<numberlocations+1;j++){
				if (polys[j].get(locMinDist)<polys[locationMinCriteria].get(locMinDist)){
					nearer=true;
				}
			}
			
			System.out.println("Poly before: "+polyID+","+lastID);
			boolean changeId=false;
			if (nearer){
				for (int j=0;j<numberlocations;j++){
					if (j!=locationMinCriteria-1){
						for (int k=0;k<allocPolys[j].size();k++){
							double id =allocPolys[j].get(k);
		//					System.out.println(j+","+locNearer+","+id+","+bufferpolys[0].indexOf(id));
							double actDist=bufferpolys[locationMinCriteria].get(bufferpolys[0].indexOf(id));
//							System.out.println(actDist+","+id);
							int polyIDbuff=bufferpolys[0].get(bufferpolys[0].indexOf(id)).intValue();
							int locNearerbuff = j;
							if (actDist<minDistance && lastID!=locNearerbuff && lastPolyID!=polyIDbuff){
								minDistance=actDist;
								polyID=polyIDbuff;
								changeId=true;
								locNearer=j;
							}
						}
					}
				}
				
			}
			System.out.println(locationMinCriteria+","+locNearer);
//			Thread.sleep(500);
			
			lastID=locNearer;
			lastPolyID=polyID;
			System.out.println("write "+polyID+" to "+(locationMinCriteria));
			allocPolys[locationMinCriteria-1].add(polyID);
			String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
			geomAllocPolys[locationMinCriteria-1].add(geometry);
			
			if (nearer && changeId==true){
//				System.out.println(allocPolys[locNearer]);
				allocPolys[locNearer].remove(Integer.valueOf(polyID));
				geomAllocPolys[locNearer].remove(geometry);
				criteria=functions.addToCriteria(polyID, locationMinCriteria, locNearer+1, criteria, true, polysGeometry);
//				System.out.println(allocPolys[locNearer]);
			}
			else{
				criteria=functions.addToCriteria(polyID, locationMinCriteria, locNearer+1, criteria, false, polysGeometry);
			}
			
			if (!nearer || changeId==false){
				for (int j=0; j<numberlocations;j++){
					polys[j+1].remove(locMinDist);
				}
			
				polys[0].remove(Double.valueOf(polyID));
			}
			
			System.out.println(polys[0].size());
		}
		
	}
	
	public static void main(String[] args)
	throws Exception {

		boolean PLZ5=true;
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
//		functions.createFileWriterLocs(numberlocations, lonlats);
		
		functions.createShapeWriter();
		functions.writeLocationsShape(numberlocations, lonlats);

		
		//initialize variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		polys = (ArrayList<Double>[])new ArrayList[numberlocations+1];
		for(int i=0;i<polys.length;i++) polys[i] = new ArrayList<Double>();
		
		bufferpolys = (ArrayList<Double>[])new ArrayList[numberlocations+1];
		for(int i=0;i<bufferpolys.length;i++) bufferpolys[i] = new ArrayList<Double>();
		
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
		
//		functions.setCriteria(criteria, numberlocations);
//		allocPolys=functions.checkthreshold(numberlocations, allocPolys, polys, polysGeometry);
//		criteria=functions.getCriteria();
		
//		//Create Shapefile with allocated poylgons
		for (int i=0; i<numberlocations;i++){
//			functions.writePolygon(output, allocPolys[i], geomAllocPolys[i],i+1);
			functions.writePolygonShape(allocPolys[i], geomAllocPolys[i],i+1,criteria);
		}
		
		functions.closeShape();
		
//		System.out.println("1:"+criteria[0]+",2:"+criteria[1]+"3:"+criteria[2]+"4:"+criteria[3]);
//		System.out.println("1:"+criteria[0]+",2:"+criteria[1]);
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");
//		
//		output.flush();
//		output.close();
	    
	    System.out.println("successfully ended");
}
	
//	static class Distances{
//		int id;
//		
//	}
}
