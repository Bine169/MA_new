
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class functions {
	static double[] criteriaf;

	public static void setCriteria(double[] crit, int numberlocations){
		criteriaf = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteriaf[i]=crit[i];
		}
	}
	
	public static double[] getCriteria(){
		return criteriaf;
	}
	
	public static double[] setLocations(int numberlocations){
		double lonlats[]= new double[numberlocations*2];
//		lonlats[4]=13.475955;			//Meißen
//		lonlats[5]=51.162388; 
//
		lonlats[0]=13.817904;			//Dresden: Leuben
		lonlats[1]=50.990689; 	
		
		lonlats[2]=13.712454;			//Dresden: Trachau
		lonlats[3]=51.085643; 
		
		lonlats[4]=13.789752;			//Dresden:Klotsche
		lonlats[5]=51.119954; 
		
		lonlats[6]=13.705294;			//Dresden: Plauen
		lonlats[7]=51.036696;

//		
//		lonlats[4]=13.682163;			//Moritzburg
//		lonlats[5]=51.155044; 
////		
//		lonlats[6]=13.904042;			//Königsbrück
//		lonlats[7]=51.264285;
		
		return lonlats;
	}
	
	//	
//	/**
//	 * calculates the number of polygons which are inside the Area; it is necessary to allocate all polygons
//	 * @return number of polygons
//	 * @throws SQLException
//	 */
	public static int getNrOrSum(boolean number) throws SQLException, ClassNotFoundException{
		Statement stmt = getConnection();
		StringBuffer sb = new StringBuffer();
		if (number)
			{ sb.append("SELECT COUNT (id) FROM criterias");}
		else
			{ sb.append("SELECT SUM(_c1) FROM criterias");}
		ResultSet t=stmt.executeQuery(sb.toString());
		t.next();
		int sum=t.getInt(1);
		if (number)
			{System.out.println("numberofpolygons: "+sum);}
		else
			{System.out.println("sum: "+sum);}

		return sum;
	}
//	
//	
//	/**
//	 * Write Polygons into shape, just necessary for testing purposes
//	 * @param buffershp: name of the ShapeBuffer
//	 * @param shpWriter: name of the ShapeWriter
//	 * @param bufferPoly: IDs of Polygons which belong/are allocated to the location
//	 * @param geomPoly: Geometries of Polygons which belong/are allocated to the location
//	 * @param location
//	 * @throws Exception
//	 */
	public static void writePolygon(FileWriter output, List<Integer> bufferPoly, List<String> geomPoly, int location) throws Exception{
	System.out.println("size"+location+" :"+bufferPoly.size());
		
	for (int i=0; i<bufferPoly.size();i++){
		output.append(Objects.toString(bufferPoly.get(i)));
		output.append(";");
		output.append(Objects.toString(location));
		output.append("\n");
	}	
	}

//	/**
//	 * Calculates the distance of the location to the actual polygon
//	 * @param location
//	 * @param geometry: geometry of the polygon to which the distance should be calculated
//	 * @param jdbc: JDBCConnection
//	 * @return double value of calculated distance
//	 * @throws SQLException
//	 */
	private static double calculateDist(StringBuffer sb, Statement stmt) throws SQLException{
		double distance;
		ResultSet d=stmt.executeQuery(sb.toString());
		d.next();
		distance=d.getDouble(1);
		
		return distance;
	}
	
	public static double calculateNeighbors(String geometrySource, String geometryTarget, Statement stmt) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('"+geometrySource+"'),ST_GeomFromText('"+geometryTarget+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	public static double calculateDistance(int poscoords, String geometry, Statement stmt, double[] lonlats) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('POINT("+lonlats[poscoords]+" "+lonlats[poscoords+1]+")'),ST_GeomFromText('"+geometry+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	public static Statement getConnection(){
		Connection jdbc = null;
		Statement stmt = null;
		try {
	         Class.forName("org.postgresql.Driver");
	         jdbc = DriverManager
	            .getConnection("jdbc:postgresql://localhost:5432/MA",
	            "postgres", "");
	      System.out.println("Opened database successfully");
	      stmt = jdbc.createStatement();
		}
	    catch ( Exception e ) {
	          System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	          System.exit(0);
	    }
		
		return stmt;
	}
	
	public static Statement getConnectionMicrom(){
		Connection jdbc = null;
		Statement stmt = null;
		try {
	         Class.forName("org.postgresql.Driver");
	         jdbc = DriverManager
	            .getConnection("jdbc:postgresql://192.168.106.51/db209789",
	            "mapchart", "3ivfbGFiB3");
	      System.out.println("Opened database successfully");
	      stmt = jdbc.createStatement();
		}
	    catch ( Exception e ) {
	          System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	          System.exit(0);
	    }
		
		return stmt;
	}
	
	public static FileWriter createFileWriter() throws IOException{
		FileWriter output = new FileWriter("polygones.csv");
		output.append(new String("ID,Location,geometry"));
		output.append("\n");
		
		return output;
	}
	
	public static void createFileWriterLocs(int numberlocations, double[] lonlats) throws IOException{
		FileWriter outputloc = new FileWriter("locations.csv");
		outputloc.append(new String("ID, Long, Lat"));
		outputloc.append("\n");
		
		int coordpos=0;
		for (int i=1; i<numberlocations+1;i++){
			outputloc.append("i,");
			outputloc.append(Objects.toString(lonlats[coordpos]));
			outputloc.append(",");
			outputloc.append(Objects.toString(lonlats[coordpos+1]));
			outputloc.append("\n");
			coordpos=coordpos+2;
		}
		outputloc.close();
	}

	public static double[] addToCriteria(int polyID, int location, int locationMaxCriteria, double[] criteria, boolean rearranged, ArrayList<String>[] polysGeometry) throws SQLException{
		//get criteria of the given polygon
		double critValue = Double.parseDouble(polysGeometry[2].get(polysGeometry[0].indexOf(String.valueOf(polyID))));
		
		if (!rearranged){
			criteria[location-1]=criteria[location-1]+critValue;
		}
		else{			
//			System.out.println("criterias before: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
			criteria[locationMaxCriteria-1]=criteria[locationMaxCriteria-1]-critValue;
			criteria[location-1]=criteria[location-1]+critValue;
//			System.out.println("criterias after: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
		}
		
		return criteria;
	}
	public static ArrayList<Integer>[] rearrangeFromBiggest(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException{

		double[] criteriaBuffer = new double[numberlocations];
		for (int j=0; j<criteriaBuffer.length;j++){
			criteriaBuffer[j]=criteriaf[j];
		}
		
		//neues array notwendig für criteria!!!!
		
		for (int j = 0; j < criteriaBuffer.length - 1; j++)
        {
            int index = j;
            for (int k = j + 1; k < criteriaBuffer.length; k++)
                if (criteriaBuffer[k] > criteriaBuffer[index])
                    index = k;
            double greaterNumber = criteriaBuffer[index]; 
            criteriaBuffer[index] = criteriaBuffer[j];
            criteriaBuffer[j] = greaterNumber;
        }
		
		//criteriaBuffer[0] area with biggest critSum
		//detect neighbours of this area
		
		List<Integer> actNeighbours = new ArrayList<Integer>();
		int locBiggest=-1;
		
		//determine location with biggest critSum
		for (int j=0;j<numberlocations;j++){
			if (criteriaf[j]==criteriaBuffer[0]){
				locBiggest=j;
			}
		}
		
		//determine neighbours of area of location with biggest critSum
		for (int j=0;j<numberlocations;j++){
			if (j!=locBiggest){
				boolean neighbour=false;
				int pos=0;
				
				System.out.println(locBiggest);
				
				while (pos<allocPolys[locBiggest].size() && neighbour==false){
					//check every allocated Polygone whether it is a neighbour of one of the polys of another location
					//take poly of locBiggest and check to every poly of loc
					
					int actPoly = allocPolys[locBiggest].get(pos);
					int posActPoly = neighbourPolyIds.indexOf(actPoly);
					boolean neighbourfound=false;
					
					for(int k=0;k<allocPolys[j].size();k++){
						int comparePoly = allocPolys[j].get(k);
						for (int l=0;l<neighbourNeighbours[posActPoly].size();l++){
							if (neighbourNeighbours[posActPoly].get(l).equals(comparePoly) && !neighbourfound){
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
		
		System.out.println(actNeighbours);
		
		//determine that area of neighbours areas with smallest critSum
		double minsum=-1;
		boolean first=true;
		int locMinsum=-1;
		
		for(int j=0;j<numberlocations;j++){
			boolean found=false;
			int posLoc=0;
			while (!found){
				if (actNeighbours.get(posLoc)==j){
					if (first){
						first=false;
						minsum=criteriaf[j];
						locMinsum=j;
					}
					else{
						if (criteriaf[j]<minsum){
							locMinsum=j;
							minsum=criteriaf[j];
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
		
		//give locMinSum 1 polygone of locbiggest
		System.out.println("neighbours:"+actNeighbours);
		System.out.println("smallest "+(locMinsum+1)+","+minsum);
		
		int polyID = allocPolys[locBiggest].get(0).intValue();
		System.out.println(polyID);
		double minDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(polyID)));
		
		for (int l=1;l<allocPolys[locBiggest].size();l++){
			double actDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(allocPolys[locBiggest].get(l))));
			if (actDist<minDist){
				minDist=actDist;
				polyID=allocPolys[locBiggest].get(l).intValue();
			}
		}
		
		//add polyID to locMinsum and remove from locbiggest
		System.out.println("Set to "+(locMinsum+1)+" remove "+polyID+" from "+(locBiggest+1));
		allocPolys[locBiggest].remove(Integer.valueOf(polyID));
		allocPolys[locMinsum].add(polyID);
//		System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
		criteriaf=addToCriteria(polyID, locMinsum+1, locBiggest+1, criteriaf, true,polysGeometry);
	
		return allocPolys;
	}
	
	public static ArrayList<Integer>[] rearrangeSmallest(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry, List<Integer> tempbufferlist, int i) throws SQLException{
		double[] criteriaBuffer = new double[numberlocations];
		for (int j=0; j<criteriaBuffer.length;j++){
			criteriaBuffer[j]=criteriaf[j];
		}
		
		//neues array notwendig für criteria!!!!
		
		for (int j = 0; j < criteriaBuffer.length - 1; j++)
        {
            int index = j;
            for (int k = j + 1; k < criteriaBuffer.length; k++)
                if (criteriaBuffer[k] < criteriaBuffer[index])
                    index = k;
            double greaterNumber = criteriaBuffer[index]; 
            criteriaBuffer[index] = criteriaBuffer[j];
            criteriaBuffer[j] = greaterNumber;
        }
		
		int posLoc=-1;
		for (int j=0;j<numberlocations;j++){
			if (criteriaBuffer[0]==criteriaf[j]){
				posLoc=j;
			}
		}
		
		i=posLoc;
		
		for (int j=0;j<numberlocations;j++){
			if (i!=j){
				for(int k=0;k<allocPolys[j].size();k++){
					tempbufferlist.add(allocPolys[j].get(k));
				}
			}
		}
//		System.out.println(tempbufferlist);
		
		double minDistance=polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(0))));
		int polyID=polys[0].get(0).intValue();
		for (int j=0;j<tempbufferlist.size();j++){
//			System.out.println(tempbufferlist.get(j)+","+polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))+","+polys[0].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j))))+","+polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))));
			double actdist=polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j))));
//			System.out.println(minDistance);
			if (actdist<minDistance){
				minDistance=actdist;
				polyID=tempbufferlist.get(j);
			}	
		}
//		System.out.println(polyID);
		int locationremove=0;
		int j=0;
		boolean notfound=true;
		while (j<numberlocations && notfound==true){
			if (j!=i){
//				System.out.println(j+","+allocPolys[j]);
				for (int k=0;k<allocPolys[j].size();k++){
					if (allocPolys[j].get(k).equals(polyID)){
						locationremove=j;
						notfound=false;
					}
				}
			}
			
			j++;
		}
//		System.out.println(allocPolys[locationremove]);
		System.out.println("Set to "+(i+1)+" remove "+polyID+" from "+(locationremove+1)+","+allocPolys[locationremove].size()+","+allocPolys[i].size());
		allocPolys[locationremove].remove(Integer.valueOf(polyID));
		allocPolys[i].add(polyID);
//		System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
		criteriaf=addToCriteria(polyID, i+1, locationremove+1, criteriaf,true,polysGeometry);
		tempbufferlist.clear();
		
		return allocPolys;
	}
	
	public static ArrayList<Integer>[] checkthresholdCombi(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException{
		boolean satisfied=false;
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		
		int lastPolyID=-1;
		int lastArea=-1;
		int run=0;
		
		while (!satisfied){
			int threshold = 15;
			int[] compCriterias = new int[numberlocations];
			for (int i=0; i<numberlocations; i++) compCriterias[i]=0;
			
			for (int i=0; i<numberlocations;i++){
				for (int j=0;j<numberlocations;j++){
					if (j!=i){
						if (criteriaf[i]<criteriaf[j]){
							if (100-(criteriaf[j]*100/criteriaf[i])>-threshold)
								compCriterias[i]++;
						}
						else{
							if ((criteriaf[j]*100/criteriaf[i])-100< threshold)
								compCriterias[i]++;
						}

					}
				}
			}
			
			int no=0;
			
			for (int i=0; i<numberlocations;i++){			
				if (compCriterias[i]!=(numberlocations-1)){
					
					if (run%2==1){
						System.out.println("biggest");
						allocPolys=rearrangeFromBiggest(numberlocations, allocPolys, polys, polysGeometry, neighbourPolyIds, neighbourNeighbours);
					}
					else{
						System.out.println("smallest");
						allocPolys=rearrangeSmallest(numberlocations, allocPolys, polys, polysGeometry, tempbufferlist, i);
					}
					
				}
				else no++;
			}
			run++;
			
			if (no==numberlocations){
				satisfied=true;
			}
		}
		return allocPolys;
	}
	
	public static ArrayList<Integer>[] checkthresholdBiggest(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException{
		boolean satisfied=false;
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		
		int lastPolyID=-1;
		int lastArea=-1;
		while (!satisfied){
			int threshold = 10;
			int[] compCriterias = new int[numberlocations];
			for (int i=0; i<numberlocations; i++) compCriterias[i]=0;
			
			for (int i=0; i<numberlocations;i++){
				for (int j=0;j<numberlocations;j++){
					if (j!=i){
						if (criteriaf[i]<criteriaf[j]){
							if (100-(criteriaf[j]*100/criteriaf[i])>-threshold)
								compCriterias[i]++;
						}
						else{
							if ((criteriaf[j]*100/criteriaf[i])-100< threshold)
								compCriterias[i]++;
						}

					}
				}
			}
			
			int no=0;
			
			for (int i=0; i<numberlocations;i++){			
				if (compCriterias[i]!=(numberlocations-1)){
					double[] criteriaBuffer = new double[numberlocations];
					for (int j=0; j<criteriaBuffer.length;j++){
						criteriaBuffer[j]=criteriaf[j];
					}
					
					//neues array notwendig für criteria!!!!
					
					for (int j = 0; j < criteriaBuffer.length - 1; j++)
			        {
			            int index = j;
			            for (int k = j + 1; k < criteriaBuffer.length; k++)
			                if (criteriaBuffer[k] > criteriaBuffer[index])
			                    index = k;
			            double greaterNumber = criteriaBuffer[index]; 
			            criteriaBuffer[index] = criteriaBuffer[j];
			            criteriaBuffer[j] = greaterNumber;
			        }
					
					//criteriaBuffer[0] area with biggest critSum
					//detect neighbours of this area
					
					List<Integer> actNeighbours = new ArrayList<Integer>();
					int locBiggest=-1;
					
					//determine location with biggest critSum
					for (int j=0;j<numberlocations;j++){
						if (criteriaf[j]==criteriaBuffer[0]){
							locBiggest=j;
						}
					}
					
					//determine neighbours of area of location with biggest critSum
					for (int j=0;j<numberlocations;j++){
						if (j!=locBiggest){
							boolean neighbour=false;
							int pos=0;
							
							System.out.println(locBiggest);
							
							while (pos<allocPolys[locBiggest].size() && neighbour==false){
								//check every allocated Polygone whether it is a neighbour of one of the polys of another location
								//take poly of locBiggest and check to every poly of loc
								
								int actPoly = allocPolys[locBiggest].get(pos);
								int posActPoly = neighbourPolyIds.indexOf(actPoly);
								boolean neighbourfound=false;
								
								for(int k=0;k<allocPolys[j].size();k++){
									int comparePoly = allocPolys[j].get(k);
									for (int l=0;l<neighbourNeighbours[posActPoly].size();l++){
										if (neighbourNeighbours[posActPoly].get(l).equals(comparePoly) && !neighbourfound){
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
					
					System.out.println(actNeighbours);
					
					//determine that area of neighbours areas with smallest critSum
					double minsum=-1;
					boolean first=true;
					int locMinsum=-1;
					
					for(int j=0;j<numberlocations;j++){
						boolean found=false;
						int posLoc=0;
						while (!found){
							if (actNeighbours.get(posLoc)==j){
								if (first){
									first=false;
									minsum=criteriaf[j];
									locMinsum=j;
								}
								else{
									if (criteriaf[j]<minsum){
										locMinsum=j;
										minsum=criteriaf[j];
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
					
					//give locMinSum 1 polygone of locbiggest
					System.out.println("neighbours:"+actNeighbours);
					System.out.println("smallest "+(locMinsum+1)+","+minsum);
					
					int polyID = allocPolys[locBiggest].get(0).intValue();
					System.out.println(polyID);
					double minDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(polyID)));
					
					for (int l=1;l<allocPolys[locBiggest].size();l++){
						double actDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(allocPolys[locBiggest].get(l))));
						if (actDist<minDist){
							minDist=actDist;
							polyID=allocPolys[locBiggest].get(l).intValue();
						}
					}
					
					//add polyID to locMinsum and remove from locbiggest
					System.out.println("Set to "+(locMinsum+1)+" remove "+polyID+" from "+(locBiggest+1));
					allocPolys[locBiggest].remove(Integer.valueOf(polyID));
					allocPolys[locMinsum].add(polyID);
//					System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
					criteriaf=addToCriteria(polyID, locMinsum+1, locBiggest+1, criteriaf, true,polysGeometry);
				}
				else no++;
			}
			
			if (no==numberlocations){
				satisfied=true;
			}
		}
		return allocPolys;
	
}

	public static ArrayList<Integer>[] checkthresholdSmallest(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry) throws SQLException{
		boolean satisfied=false;
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		
		int lastPolyID=-1;
		int lastArea=-1;
		while (!satisfied){
			int threshold = 10;
			int[] compCriterias = new int[numberlocations];
			for (int i=0; i<numberlocations; i++) compCriterias[i]=0;
			
			for (int i=0; i<numberlocations;i++){
				for (int j=0;j<numberlocations;j++){
					if (j!=i){
						if (criteriaf[i]<criteriaf[j]){
							if (100-(criteriaf[j]*100/criteriaf[i])>-threshold)
								compCriterias[i]++;
						}
						else{
							if ((criteriaf[j]*100/criteriaf[i])-100< threshold)
								compCriterias[i]++;
						}

					}
				}
			}
			
			int no=0;
			
			for (int i=0; i<numberlocations;i++){
				if (compCriterias[i]!=(numberlocations-1)){
					
					for (int j=0;j<numberlocations;j++){
						if (i!=j && j!=lastArea){
							for(int k=0;k<allocPolys[j].size();k++){
								tempbufferlist.add(allocPolys[j].get(k));
							}
						}
					}
//					System.out.println(tempbufferlist);
					
					double minDistance=polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(0))));
					int polyID=polys[0].get(0).intValue();
					for (int j=0;j<tempbufferlist.size();j++){
//						System.out.println(tempbufferlist.get(j)+","+polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))+","+polys[0].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j))))+","+polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))));
						double actdist=polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j))));
//						System.out.println(minDistance);
						if (actdist<minDistance && lastPolyID!=tempbufferlist.get(j)){
							minDistance=actdist;
							polyID=tempbufferlist.get(j);
						}	
					}
//					System.out.println(polyID);
					
					lastPolyID=polyID;
					int locationremove=0;
					int j=0;
					boolean notfound=true;
					while (j<numberlocations && notfound==true){
						if (j!=i){
//							System.out.println(j+","+allocPolys[j]);
							for (int k=0;k<allocPolys[j].size();k++){
								if (allocPolys[j].get(k).equals(polyID)){
									locationremove=j;
									notfound=false;
								}
							}
						}
						
						j++;
					}
					
					lastArea=i;
//					System.out.println(allocPolys[locationremove]);
					System.out.println("Set to "+(i+1)+" remove "+polyID+" from "+(locationremove+1)+","+allocPolys[locationremove].size()+","+allocPolys[i].size());
					allocPolys[locationremove].remove(Integer.valueOf(polyID));
					allocPolys[i].add(polyID);
//					System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
					criteriaf=addToCriteria(polyID, i+1, locationremove+1, criteriaf,true,polysGeometry);
					tempbufferlist.clear();
				}
				else no++;
			}
			
			if (no==numberlocations){
				satisfied=true;
			}
		}
		return allocPolys;
	}
	
}
