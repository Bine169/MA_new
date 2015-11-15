package MA;

import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.mapchart.common.io.jdbc.JDBCConnection;

public class CritDistInOutTrueNearest {
	
	private static double lonlats[]; //stores locations
	private static ArrayList<Double>[] polys; //stores ID and distances to location
	private static ArrayList<Double>[] bufferpolys; //stores ID and distances to location
	private static ArrayList<String>[] polysGeometry; //stores ID and geometry of polys
	private static ArrayList<Integer>[] allocPolys; //stores allocated polys dependent on location 
	private static ArrayList<String>[] geomAllocPolys; //stores geometries of allocated polygons
	
	private static class polyNeighbours{
		static List<Integer> polyIds;
		static ArrayList<Integer>[] neighbours;
	}
	
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
		String tablegeom=null;
		String tablecrit=null;
		
		if (PLZ5){
			columnIDs="_g7304";
			tablegeom="geometries";
			tablecrit="criterias";
		}
		else{
		//PLZ8
			columnIDs="_g7305";
			tablegeom="geometriesplz8";
			tablecrit="criteriasplz8";
		}
	
		//get all PolygonIDs and store it
		StringBuffer sb = new StringBuffer();
		//SELECT t2.id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM _varea_1424340553765 AS t1 INNER JOIN _vcriteria_1424340553765 As t2 ON t2._g7304=t1.id
		sb.append("SELECT t2.id AS id, ST_AsTEXT(the_geom) AS the_geom, _c1 AS criteria FROM "+tablegeom+" AS t1 INNER JOIN "+tablecrit+" AS t2 ON t2."+columnIDs+"=t1.id");
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
			
			int polyID=polys[0].get(i).intValue();
			polyNeighbours.polyIds.add(polyID);
			
			//nearest neighbour with distances
//			for (int j=0; j<polys[0].size();j++){
//				if (j!=i){
//					String geometryTarget = polysGeometry[1].get(j);
//					double distance = functions.calculateNeighbors(geometry, geometryTarget, jdbc);
//					if (distance==0){
//						polyNeighbours.neighbours[i].add(polys[0].get(j).intValue());
//					}
//				}
//			}
			
			ResultSet nN = functions.getNearestNeighbours(polyID, tablegeom, jdbc);
			boolean last=false;
			while (!last){
				nN.next();
				polyNeighbours.neighbours[i].add(nN.getInt(1));
				if (nN.isLast()){last=true;}
			}
		}
			
		System.out.println("length"+polys[0].size());
		int lastID=-1;
		int lastPolyID=-1;
		int actPolyID=-1;
		int locGet =-1;
		List<Integer> actNeighbours = new ArrayList<Integer>();
		
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
			actPolyID = polyID;
			System.out.println(polyID);
			
			boolean nearer=false;
			//check whether Poly is nearer to another one
			int locNearer=-1;
			for (int j=1;j<numberlocations+1;j++){
				if (polys[j].get(locMinDist)<polys[locationMinCriteria].get(locMinDist)){
					nearer=true;
					locNearer=j-1;
				}
			}
			
			System.out.println("Poly before: "+polyID+","+lastID);
			System.out.println("locationMinCriteria:"+locationMinCriteria);
			boolean changeId=false;
			if (nearer){
				System.out.println("1:"+polys[0].size()+","+polys[1].size());
//				for (int j=0;j<numberlocations;j++){
//					if (j!=locationMinCriteria-1){
//						for (int k=0;k<allocPolys[j].size();k++){
//							double id =allocPolys[j].get(k);
//		//					System.out.println(j+","+locNearer+","+id+","+bufferpolys[0].indexOf(id));
//							double actDist=bufferpolys[locationMinCriteria].get(bufferpolys[0].indexOf(id));
////							System.out.println(actDist+","+id);
//							int polyIDbuff=bufferpolys[0].get(bufferpolys[0].indexOf(id)).intValue();
//							int locNearerbuff = j;
//							if (actDist<minDistance && lastID!=locNearerbuff && lastPolyID!=polyIDbuff){
//								minDistance=actDist;
//								polyID=polyIDbuff;
//								changeId=true;
//								locNearer=j;
//							}
//						}
//					}
//				}
				
				//-------------------------
				//determine neighbours of area of location with biggest critSum
				for (int j=0;j<numberlocations;j++){
					if (j!=locMinDist){
						boolean neighbour=false;
						int pos=0;
						
//						System.out.println(locBiggest);
//						System.out.println(pos+","+allocPolys[locationMinCriteria-1].size());
						
						while (pos<allocPolys[locationMinCriteria-1].size() && neighbour==false){
							//check every allocated Polygone whether it is a neighbour of one of the polys of another location
							//take poly of locBiggest and check to every poly of loc
							
							int actPoly = allocPolys[locationMinCriteria-1].get(pos);
							int posActPoly = polyNeighbours.polyIds.indexOf(actPoly);
							boolean neighbourfound=false;
							
							for(int k=0;k<allocPolys[j].size();k++){
								int comparePoly = allocPolys[j].get(k);
								for (int l=0;l<polyNeighbours.neighbours[posActPoly].size();l++){
									if (polyNeighbours.neighbours[posActPoly].get(l).equals(comparePoly) && !neighbourfound){
										neighbour=true;
										actNeighbours.add(j);
										neighbourfound=true;
									}
								}
							}
							
							pos++;
						}
					}
				}
				System.out.println("2:"+polys[0].size()+","+polys[1].size());
				
				System.out.println("neighbours:"+actNeighbours);
				System.out.println("locNearer"+locNearer);

				boolean locNearerDetermined = false;
				for (int j=0;j<actNeighbours.size();j++){
					if (actNeighbours.get(j).equals(locNearer)){
						locNearerDetermined=true;
					}
				}
				
				int locNeighbour=-1;
				
				System.out.println("3:"+polys[0].size()+","+polys[1].size());
				if (!locNearerDetermined){	
//					//determine that area of neighbours areas with biggest critSum
					double maxSum=-1;
					boolean first=true;
					int locMaxSum=-1;
					
					for(int j=0;j<numberlocations;j++){
						boolean found=false;
						int posLoc=0;
						while (!found){
							if (actNeighbours.get(posLoc)==j && j!=lastID){
								if (first){
									first=false;
									maxSum=criteria[j];
									locMaxSum=j;
								}
								else{
									if (criteria[j]<maxSum){
										locMaxSum=j;
										maxSum=criteria[j];
									}
								}
								
								found=true;
							}
							if ((posLoc+1)<actNeighbours.size()){
								posLoc++;
							}
							else{
								found=true;
							}
						}
					}
					
					locNeighbour=locMaxSum;
				}
				
				actNeighbours.clear();
				System.out.println("4:"+polys[0].size()+","+polys[1].size());
				
				if (!locNearerDetermined){locGet =locNeighbour;}
				else{ locGet=locNearer;}
				
				System.out.println(locGet);
				
				for (int j=0;j<allocPolys[locGet].size();j++){
					System.out.println("5:"+j+","+polys[0].size()+","+polys[1].size());
					double id =allocPolys[locGet].get(j);
//					System.out.println(j+","+locNearer+","+id+","+bufferpolys[0].indexOf(id));
					double actDist=bufferpolys[locationMinCriteria].get(bufferpolys[0].indexOf(id));
//					System.out.println(actDist+","+id);
					int polyIDbuff=bufferpolys[0].get(bufferpolys[0].indexOf(id)).intValue();
//					System.out.println("last:"+lastID+","+lastPolyID);
//					System.out.println("new:"+locNearer+","+polyIDbuff);
					if (actDist<minDistance && lastID!=locGet && lastPolyID!=polyIDbuff){
						minDistance=actDist;
						polyID=polyIDbuff;
						changeId=true;
//						locNearer=j;
//					}
					}
				}
				
			}
//			System.out.println(locationMinCriteria+","+locNearer);
//			Thread.sleep(500);
			
			lastID=locGet;
			lastPolyID=polyID;
			System.out.println("write "+polyID+" to "+(locationMinCriteria)+" from "+locGet);
			allocPolys[locationMinCriteria-1].add(polyID);
			String geometry = polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(polyID)));
			geomAllocPolys[locationMinCriteria-1].add(geometry);
			
			if (nearer && changeId){
//				System.out.println(allocPolys[locNearer]);
				allocPolys[locNearer].remove(Integer.valueOf(polyID));
				geomAllocPolys[locNearer].remove(geometry);
				criteria=functions.addToCriteria(polyID, locationMinCriteria, locNearer+1, criteria, true, polysGeometry);
				allocPolys[locNearer].add(actPolyID);
				geomAllocPolys[locNearer].add(polysGeometry[1].get(polysGeometry[0].indexOf(Integer.toString(actPolyID))));
				criteria=functions.addToCriteria(polyID, locNearer+1, 0, criteria, false, polysGeometry);
//				System.out.println(allocPolys[locNearer]);
			}
			else{
				criteria=functions.addToCriteria(polyID, locationMinCriteria, locNearer+1, criteria, false, polysGeometry);
			}
			
//			if (!nearer || !changeId){
				for (int j=0; j<numberlocations;j++){
					polys[j+1].remove(locMinDist);
				}
			
				polys[0].remove(Double.valueOf(actPolyID));
//			}
			
			System.out.println(polys[0].size()+","+polys[1].size());
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
		
		polyNeighbours.polyIds = new ArrayList<Integer>();
		polyNeighbours.neighbours = (ArrayList<Integer>[])new ArrayList[numberpolygons];
		for (int i=0; i<polyNeighbours.neighbours.length;i++) polyNeighbours.neighbours[i]=new ArrayList<Integer>();
		
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
