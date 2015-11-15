package MA;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mapchart.common.geo.file.shape.DBFBuffer;
import com.mapchart.common.geo.file.shape.SHPBuffer;
import com.mapchart.common.geo.file.shape.ShapeBuffer;
import com.mapchart.common.geo.file.shape.ShapeReader;
import com.mapchart.common.geo.file.shape.ShapeType;
import com.mapchart.common.geo.file.shape.ShapeWriter;
import com.mapchart.common.geom.buffer.DoubleBuffer;
import com.mapchart.common.io.jdbc.JDBCConnection;
import com.mapchart.common.io.jdbc.JDBCManager;
import com.mapchart.common.io.jdbc.JDBCTypes;
import com.mapchart.core.routing.server.Wkttests;
import com.mapchart.core.server.config.ServerGlobal;

public class functions {
	static double[] criteriaf;
	static ShapeBuffer buffershp;
	static ShapeWriter shpWriter;
	static ArrayList<String>[] geomAllocPolys;
	static ArrayList<String>[] buffgeomAllocPolys; 
	static ArrayList<Integer>[] rearrangedPolys = (ArrayList<Integer>[])new ArrayList[3];
	static int counterIdUsed=0;
	static boolean raiseThreshold=false;
	static int lastAreaSmallest;
	static int lastAreaBiggest;

	public static void setCriteria(double[] crit, int numberlocations){
		criteriaf = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteriaf[i]=crit[i];
		}
	}
	
	public static double[] getCriteria(){
		return criteriaf;
	}
	
	public static void setGeometryAllocPolys(ArrayList<String>[] geometries){
		geomAllocPolys=geometries;
	}
	
	public static ArrayList<String>[] getGeometryAllocPolys(){
		return geomAllocPolys;
	}
	
	public static double[] setLocations(int numberlocations, boolean PLZ5, boolean common) throws IOException{
		double lonlats[]= new double[numberlocations*2];
		
//		if (false){
////		lonlats[0]=13.475955;			//Mei�en
////		lonlats[1]=51.162388; 
//////
////		lonlats[2]=13.817904;			//Dresden: Leuben
////		lonlats[3]=50.990689; 	
//		
////		lonlats[2]=13.712454;			//Dresden: Trachau
////		lonlats[3]=51.085643; 
//		
////		lonlats[4]=13.789752;			//Dresden:Klotsche
////		lonlats[5]=51.119954; 
////		
////		lonlats[6]=13.705294;			//Dresden: Plauen
////		lonlats[7]=51.036696;
//
////		
////		lonlats[4]=13.682163;			//Moritzburg
////		lonlats[5]=51.155044; 
//////		
////		lonlats[6]=13.904042;			//K�nigsbr�ck
////		lonlats[7]=51.264285;
//		
//		//locations SPK
//		lonlats[0]=13.73895;			//Dresden, ID1
//		lonlats[1]=51.04765;
//		
//		lonlats[2]=13.92143;			//Radeberg, ID 41
//		lonlats[3]=51.11635;
//		
//		lonlats[4]=13.82982;			//Ottendorf-Okrilla; ID43
//		lonlats[5]=51.18561;
//		
//		lonlats[6]=13.64204;			//Rabenau, ID55
//		lonlats[7]=50.96289;
//		}
//		else{
			ShapeReader r = new ShapeReader("C:\\Users\\s.schmidt@microm-mapchart.com\\Desktop\\Praktikum\\MA\\Results\\OSD_Standorte\\OSD_Standorte.shp", "ISO-8859-1");
			int pos=0;
			
			if(common){
				boolean satisfied=false;
				int i=0;
				List<Integer> ids = new ArrayList<Integer>();
//				ids.add(5);	//DD Weixdorf
//				ids.add(10); //DD Elbcenter
				ids.add(11); //DD Löbtau
//				ids.add(29);	//DD Seidnitz
//				ids.add(34); //DD Sparkassenhaus
//				ids.add(39);	 //DD Weißig
				ids.add(41); //Radeberg Hauptstr
//				ids.add(51); //Kesselsdorf
//				ids.add(54);	//Kreischa
				ids.add(55); //Rabenau
//				ids.add(56); //Tharandt
//				ids.add(60); //Altenberg
				ids.add(68); //Struppen
				ids.add(72);	//DD Heidenau West
//				ids.add(77);	//Bergießhübel
				ids.add(79);	//Liebstadt
				ids.add(82);	//Neustadt
				ids.add(90); //Panschwitz Kuckau
				ids.add(92); //Schwepnitz
				ids.add(96); //Hoyerswerda Altstadt
				
				while (!satisfied){
					r.readDataset();
					DBFBuffer id = r.getShapeBuffer().getDbfBuffer();
					String ID = id.getFieldValue("ID");
					int IDint = Integer.valueOf(ID);
					
					if (ids.contains(IDint)){
						i++;
						SHPBuffer buffer = r.getShapeBuffer().getShpBuffer();
						DoubleBuffer coords = buffer.getCoordsReference();
						double lon = coords.getX();
						double lat = coords.getY();
						lonlats[pos]=lon;
						lonlats[pos+1]=lat;
						pos=pos+2;
					}
					
					if (i==10){satisfied=true;}
				}
			}
			else{
				
				for (int i=0; i<numberlocations;i++){
					r.readDataset();
					if (i!=45 && i!=51 && i!=50 && i!=49){
					SHPBuffer buffer = r.getShapeBuffer().getShpBuffer();
					DoubleBuffer coords = buffer.getCoordsReference();
					double lon = coords.getX();
					double lat = coords.getY();
					lonlats[pos]=lon;
					lonlats[pos+1]=lat;
					pos=pos+2;
					}
				}
				System.out.println(lonlats);
			}
			
			
//		}
		return lonlats;
	}
	
	//	
//	/**
//	 * calculates the number of polygons which are inside the Area; it is necessary to allocate all polygons
//	 * @return number of polygons
//	 * @throws SQLException
//	 */
	public static int getNrOrSum(boolean number, boolean PLZ5) throws SQLException, ClassNotFoundException{
		JDBCConnection jdbc = getConnectionMicrom();
		StringBuffer sb = new StringBuffer();
		String table=null;
		if (PLZ5){
			table="criterias";
		}
		else{
			table="criteriasplz8";
		}
		
		if (number)
			{ sb.append("SELECT COUNT (id) FROM "+table+";");
			}
		else
			{ sb.append("SELECT SUM(CAST(_c1 AS int)) FROM criterias");}
		ResultSet t=jdbc.executeQuery(sb.toString());
		t.next();
		int sum=t.getInt(1);
		if (number)
			{System.out.println("numberofpolygons: "+sum);}
		else
			{System.out.println("sum: "+sum);}
		
		JDBCManager.free(jdbc);
		jdbc = null;

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
	
	public static void writePolygonShape(List<Integer> allocPoly, List<String> geomPoly, int location,double[] criteria) throws Exception{
		System.out.println("size"+location+" :"+geomPoly.size()+","+allocPoly.size()+", criteria: "+criteria[location-1]);
		
	for (int i=0; i<geomPoly.size();i++){
		Wkttests wktReader = new Wkttests();
		wktReader.convert(geomPoly.get(i));
		
		DoubleBuffer geom = wktReader.getDoubleBuffer();
		buffershp.getShpBuffer().getCoordsReference().set(geom);
		buffershp.getDbfBuffer().setFieldValue(0, Objects.toString(allocPoly.get(i)));
		buffershp.getDbfBuffer().setFieldValue(1, Objects.toString(location));
		shpWriter.writeDataset();
	}	
	}
	
	public static void closeShape() throws IOException{
		shpWriter.close();
	}

//	/**
//	 * Calculates the distance of the location to the actual polygon
//	 * @param location
//	 * @param geometry: geometry of the polygon to which the distance should be calculated
//	 * @param jdbc: JDBCConnection
//	 * @return double value of calculated distance
//	 * @throws SQLException
//	 */
	private static double calculateDist(StringBuffer sb, JDBCConnection stmt) throws SQLException{
		double distance;
		ResultSet d=stmt.executeQuery(sb.toString());
		d.next();
		distance=d.getDouble(1);
		
		return distance;
	}
	
	public static double calculateNeighbors(String geometrySource, String geometryTarget, JDBCConnection stmt) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('"+geometrySource+"'),ST_GeomFromText('"+geometryTarget+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	public static double calculateDistance(int poscoords, String geometry, JDBCConnection stmt, double[] lonlats) throws SQLException{
		double distance;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT st_distance(ST_GeomFromText('POINT("+lonlats[poscoords]+" "+lonlats[poscoords+1]+")'),ST_GeomFromText('"+geometry+"'));");
		distance = calculateDist(sb, stmt);
		return distance;
	}
	
	public static ResultSet getNearestNeighbours(int polyID, String tablegeom, JDBCConnection jdbc) throws SQLException{
		//SELECT (pgis_fn_nn(p1.the_geom, 0.0005, 1000, 10, 'geometries', 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT st_geomfromtext((Select st_astext(the_geom) FROM geometries WHERE ID=1), 4326) AS the_geom) AS p1;
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT (pgis_fn_nn(p1.the_geom, 0.0, 1000, 10, '"+tablegeom+"', 'true', 'id', 'the_geom' )).nn_gid::int FROM (SELECT st_geomfromtext((Select st_astext(the_geom) FROM "+tablegeom+" WHERE ID="+polyID+"), 4326) AS the_geom) AS p1;");
		ResultSet rNeighbours=jdbc.executeQuery(sb.toString());
		
		return rNeighbours;
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
	
	public static JDBCConnection initConnectionMicrom(boolean PLZ5, boolean common) throws SQLException{
		JDBCConnection jdbc = null;

		try {
//			ServerGlobal.POSTGRESQL_DB_ROUTING = "db209789";
			ServerGlobal.POSTGRESQL_DB_ROUTING = "db209789_2";
			JDBCManager.addPool(JDBCTypes.POSTGRESQL, "192.168.106.51", "mapchart", "3ivfbGFiB3", 2, false);
			
			//Test data Dresden, Create Table with that data
			
			jdbc = JDBCManager.getConnection(JDBCTypes.POSTGRESQL, ServerGlobal.POSTGRESQL_DB_ROUTING);
	        System.out.println("Opened database successfully");
		}
	    catch ( Exception e ) {
	          System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	          System.exit(0);
	    }
		
		StringBuffer sb = new StringBuffer();

		//PLZ5
		if (PLZ5){
//			sb.append("CREATE TEMPORARY VIEW geometries As SELECT * FROM _g7304 WHERE geocode<'03000';");
//			sb.append("CREATE TEMPORARY VIEW criterias As SELECT * FROM _a53444 WHERE geocode<'03000';");
			
//			sb.append("CREATE TEMPORARY VIEW geometries As SELECT * FROM _g7304 WHERE geocode<'01478' OR (geocode>'01700' AND geocode<'01945') OR (geocode>'02000' AND geocode<'02610') OR (geocode>'02970' AND geocode<'03000');");
//			sb.append("CREATE TEMPORARY VIEW criterias As SELECT * FROM _a53444 WHERE geocode<'01478' OR (geocode>'01700' AND geocode<'01945') OR (geocode>'02000' AND geocode<'02610') OR (geocode>'02970' AND geocode<'03000');");
//			
////			sb.append("CREATE TEMPORARY VIEW geometries As SELECT * FROM _g7304 WHERE geocode<'01468' OR (geocode>'01700' AND geocode<'01723') OR (geocode>'01723' AND geocode<'01737') OR (geocode>'01744' AND geocode<'01750');");
//			sb.append("CREATE TEMPORARY VIEW criterias As SELECT * FROM _a53444 WHERE geocode<'01468' OR (geocode>'01700' AND geocode<'01723') OR (geocode>'01723' AND geocode<'01737') OR (geocode>'01744' AND geocode<'01750');");
		}
		else{
		//PLZ8
			if (common){
//				sb.append("CREATE TEMPORARY VIEW geometries As SELECT * FROM _g7305 WHERE geocode<'01478' OR (geocode>'01700' AND geocode<'01945') OR (geocode>'02000' AND geocode<'02610') OR (geocode>'02970' AND geocode<'03000');");
//				sb.append("CREATE TEMPORARY VIEW criterias As SELECT * FROM _a53350 WHERE geocode<'01478' OR (geocode>'01700' AND geocode<'01945') OR (geocode>'02000' AND geocode<'02610') OR (geocode>'02970' AND geocode<'03000');");
////				sb.append("CREATE TEMPORARY VIEW criterias As SELECT * FROM _a53350 WHERE geocode<'025000000';");
//				sb.append("CREATE TEMPORARY VIEW geometries As SELECT * FROM _g7305 WHERE geocode<'025000000';");
			}
			else{
//				sb.append("CREATE TEMPORARY VIEW criterias As SELECT * FROM _a53350 WHERE geocode<'014680000' OR (geocode>'017000000' AND geocode<'017230000') OR (geocode>'017239000' AND geocode<'017370000') OR (geocode>'017449000' AND geocode<'017500000');");
//				sb.append("CREATE TEMPORARY VIEW geometries As SELECT * FROM _g7305 WHERE geocode<'014680000' OR (geocode>'017000000' AND geocode<'017230000') OR (geocode>'017239000' AND geocode<'017370000') OR (geocode>'017449000' AND geocode<'017500000');");
				}
		}
		System.out.println(sb);
		jdbc.executeUpdate(sb.toString());
		
		JDBCManager.free(jdbc);
		jdbc = null;
		
		return jdbc;
	}
		
	public static JDBCConnection getConnectionMicrom() throws SQLException{
		JDBCConnection jdbc = null;

		try {
			jdbc = JDBCManager.getConnection(JDBCTypes.POSTGRESQL, ServerGlobal.POSTGRESQL_DB_ROUTING);
		}
	    catch ( Exception e ) {
	          System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	          System.exit(0);
	    }
		
		return jdbc;
	}
	
	public static FileWriter createFileWriter() throws IOException{
		FileWriter output = new FileWriter("polygones.csv");
		output.append(new String("ID,Location,geometry"));
		output.append("\n");
		
		return output;
	}
	
	
	public static void createShapeWriter() throws IOException{
		buffershp = new ShapeBuffer(ShapeType.POLYGON,"ISO-8859-1");
		shpWriter = new ShapeWriter("C:\\Users\\s.schmidt@microm-mapchart.com\\Desktop\\Praktikum\\MA\\Results\\catchmentAreas.shp",buffershp);
		
		buffershp.getDbfBuffer().addField("ID", 50, false);
		buffershp.getDbfBuffer().addField("Location", 50, false);
		buffershp.getDbfBuffer().addField("_c1", 60, true);
	}
	
	public static void writeLocationsShape(int numberlocations, double[] lonlats) throws IOException{
		ShapeBuffer buffershpLocation = new ShapeBuffer(ShapeType.POINT,"ISO-8859-1");
		buffershpLocation.getDbfBuffer().addField("ID", 50, false);
		ShapeWriter shpWriterLocation = new ShapeWriter("C:\\Users\\s.schmidt@microm-mapchart.com\\Desktop\\Praktikum\\MA\\Results\\location.shp",buffershpLocation);
		int coordpos=0;
		for (int i=1; i<numberlocations+1;i++){
			buffershpLocation.getShpBuffer().getCoordsReference().set(lonlats[coordpos],lonlats[coordpos+1]);
			buffershpLocation.getDbfBuffer().setFieldValue(0, Objects.toString(i));
			shpWriterLocation.writeDataset();
			coordpos=coordpos+2;
		}
		shpWriterLocation.close();
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
			System.out.println("criterias before: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
			criteria[locationMaxCriteria-1]=criteria[locationMaxCriteria-1]-critValue;
			criteria[location-1]=criteria[location-1]+critValue;
			System.out.println("criterias after: "+ criteria[locationMaxCriteria-1]+","+criteria[location-1]);
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
				
//				System.out.println(locBiggest);
				
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
		
//		System.out.println(actNeighbours);
		
		//determine that area of neighbours areas with smallest critSum
		double minsum=-1;
		boolean first=true;
		int locMinsum=-1;
		
		for(int j=0;j<numberlocations;j++){
			boolean found=false;
			int posLoc=0;
			while (!found){
				if (actNeighbours.get(posLoc)==j && j!=lastAreaBiggest){
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
//		System.out.println("neighbours:"+actNeighbours);
//		System.out.println("smallest "+(locMinsum+1)+","+minsum);
		
		int polyID = allocPolys[locBiggest].get(0).intValue();
//		System.out.println(polyID);
		double minDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(polyID)));
		
		for (int l=1;l<allocPolys[locBiggest].size();l++){
			double actDist = polys[locMinsum+1].get(polys[0].indexOf(Double.valueOf(allocPolys[locBiggest].get(l))));
			if (actDist<minDist){
				minDist=actDist;
				polyID=allocPolys[locBiggest].get(l).intValue();
			}
		}
		
		counterIdUsed=0;
		for (int k=0;k<rearrangedPolys[0].size();k++){
			if (rearrangedPolys[2].get(k).equals(locMinsum) && rearrangedPolys[1].get(k).equals(locBiggest) && rearrangedPolys[0].get(k).equals(polyID)){
				counterIdUsed++;
			}
		}
		
		lastAreaBiggest=locBiggest;
		System.out.println(counterIdUsed);
		System.out.println(rearrangedPolys[0]);
		System.out.println(rearrangedPolys[2]);
		
		rearrangedPolys[0].add(polyID);
		rearrangedPolys[1].add(locBiggest);
		rearrangedPolys[2].add(locMinsum);
		
		if (rearrangedPolys[0].size()>(numberlocations*numberlocations)){
			rearrangedPolys[0].remove(0);
			rearrangedPolys[1].remove(0);
			rearrangedPolys[2].remove(0);
		}
		
		//add polyID to locMinsum and remove from locbiggest
		System.out.println("Set to "+(locMinsum+1)+" remove "+polyID+" from "+(locBiggest+1));
		String geom = geomAllocPolys[locBiggest].get(allocPolys[locBiggest].indexOf(polyID));
		buffgeomAllocPolys[locBiggest].remove(allocPolys[locBiggest].indexOf(polyID));
		allocPolys[locBiggest].remove(Integer.valueOf(polyID));

		allocPolys[locMinsum].add(polyID);
		buffgeomAllocPolys[locMinsum].add(geom);
		
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
			if (i!=j && j!=lastAreaSmallest){
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
		
		counterIdUsed=0;
		for (int k=0;k<rearrangedPolys[0].size();k++){
			if (rearrangedPolys[2].get(k).equals(i) && rearrangedPolys[1].get(k).equals(locationremove) && rearrangedPolys[0].get(k).equals(polyID)){
				counterIdUsed++;
			}
		}
		
		rearrangedPolys[0].add(polyID);
		rearrangedPolys[1].add(locationremove);
		rearrangedPolys[2].add(i);
		
		if (rearrangedPolys[0].size()>(numberlocations)){
			rearrangedPolys[0].remove(0);
			rearrangedPolys[1].remove(0);
			rearrangedPolys[2].remove(0);
		}
		
		lastAreaSmallest=i;
//		System.out.println(allocPolys[locationremove]);
		System.out.println("Set to "+(i+1)+" remove "+polyID+" from "+(locationremove+1)+","+allocPolys[locationremove].size()+","+allocPolys[i].size());
		
		String geom = geomAllocPolys[locationremove].get(allocPolys[locationremove].indexOf(polyID));
		buffgeomAllocPolys[locationremove].remove(allocPolys[locationremove].indexOf(polyID));
		allocPolys[locationremove].remove(Integer.valueOf(polyID));

		allocPolys[i].add(polyID);
		buffgeomAllocPolys[i].add(geom);
		
//		System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
		criteriaf=addToCriteria(polyID, i+1, locationremove+1, criteriaf,true,polysGeometry);
		tempbufferlist.clear();
		
		return allocPolys;
	}
	
	public static ArrayList<Integer>[] checkthresholdCombi(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException{
		boolean satisfied=false;
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		for(int i=0;i<rearrangedPolys.length;i++) rearrangedPolys[i] = new ArrayList<Integer>();
		
		ArrayList<Integer>[] buffallocPolys; 
		buffallocPolys=allocPolys;
		buffgeomAllocPolys=geomAllocPolys;
		
		lastAreaBiggest=-1;
		lastAreaSmallest=-1;
		int lastArea=-1;
		int run=0;
		int threshold = 10;
		
		while (!satisfied){
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
			boolean arranged=false;
			
			for (int i=0; i<numberlocations;i++){			
				if (compCriterias[i]!=(numberlocations-1) && !arranged){
					
					if (run%2==1){
						System.out.println("biggest");
						allocPolys=rearrangeFromBiggest(numberlocations, buffallocPolys, polys, polysGeometry, neighbourPolyIds, neighbourNeighbours);
					}
					else{
						System.out.println("smallest");
						allocPolys=rearrangeSmallest(numberlocations, buffallocPolys, polys, polysGeometry, tempbufferlist, i);
					}
					
					if (counterIdUsed>=numberlocations){
						counterIdUsed=0;
						rearrangedPolys[0].clear();
						rearrangedPolys[1].clear();
						rearrangedPolys[2].clear();
						raiseThreshold=true;
					}
					
					arranged=true;
				}
				else no++;
			}
			run++;
			
			if (no==numberlocations){
				satisfied=true;
			}
			
			if (raiseThreshold && !satisfied){
				threshold=threshold+5;
				raiseThreshold=false;
				buffallocPolys=allocPolys;
				buffgeomAllocPolys=geomAllocPolys;
				System.out.println("Threshold raised to "+threshold);
			}
		}
		
		allocPolys=buffallocPolys;
		geomAllocPolys=buffgeomAllocPolys;
		
		System.out.println("rearranged with a variance of "+threshold+"%");
		return allocPolys;
	}
	
	public static ArrayList<Integer>[] checkthresholdBiggest(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry,List<Integer> neighbourPolyIds, ArrayList<Integer>[] neighbourNeighbours) throws SQLException{
		boolean satisfied=false;
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		
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
							if (actNeighbours.get(posLoc)==j && lastAreaBiggest!=j){
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
					
					lastAreaBiggest=locBiggest;
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
						if (i!=j && j!=lastAreaSmallest){
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
					lastAreaSmallest=i;
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
	
	public static ArrayList<Integer>[] checkthreshold(int numberlocations, ArrayList<Integer>[] allocPolys, ArrayList<Double>[] polys, ArrayList<String>[] polysGeometry) throws SQLException, InterruptedException{
		boolean satisfied=false;
		List<Integer> tempbufferlist = new ArrayList<Integer>();
		
		for(int i=0;i<rearrangedPolys.length;i++) rearrangedPolys[i] = new ArrayList<Integer>();
		
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
			int counterIdUsed=0;
			boolean notarranged=true;
			
			double[] criteriaBuffer = new double[numberlocations];
			for (int i=0; i<criteriaBuffer.length;i++){
				criteriaBuffer[i]=criteriaf[i];
			}
			
			for (int i = 0; i < criteriaBuffer.length - 1; i++)
	        {
	            int index = i;
	            for (int j = i + 1; j < criteriaBuffer.length; j++)
	                if (criteriaBuffer[j] < criteriaBuffer[index])
	                    index = j;
	            double greaterNumber = criteriaBuffer[index]; 
	            criteriaBuffer[index] = criteriaBuffer[i];
	            criteriaBuffer[i] = greaterNumber;
	        }
			
			
			for (int i=0; i<numberlocations;i++){
				
				if (notarranged){
				
				int posCrit=-1;
				
				for (int k=0;k<numberlocations;k++){
					if (criteriaBuffer[i]==criteriaf[k]){
						posCrit=k;
					}
				}
				
				System.out.println(posCrit);
//				Thread.sleep(5000);
				if (compCriterias[posCrit]!=(numberlocations-1)){
					
					System.out.println(lastArea+","+posCrit);
					for (int j=0;j<numberlocations;j++){
						if (posCrit!=j && lastArea!=j){
							for(int k=0;k<allocPolys[j].size();k++){
								tempbufferlist.add(allocPolys[j].get(k));
							}
						}
					}
//					System.out.println(tempbufferlist);
					
					double minDistance=polys[posCrit+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(0))));
					int polyID=tempbufferlist.get(0);
//					System.out.println("first PolyID:"+polyID);
					for (int j=0;j<tempbufferlist.size();j++){
//						System.out.println(tempbufferlist.get(j)+","+polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))+","+polys[0].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j))))+","+polys[i+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j)))));
						double actdist=polys[posCrit+1].get(polys[0].indexOf(Double.valueOf(tempbufferlist.get(j))));
//						System.out.println(minDistance);
						boolean notUsed =true;
//						
//						for (int k=0;k<rearrangedPolys[0].size();k++){
////							if (tempbufferlist.get(j).equals(2)){
////							System.out.println(rearrangedPolys[0].get(k)+","+rearrangedPolys[2].get(k)+","+i+","+tempbufferlist.get(j)+","+notUsed+","+polyID);
////							Thread.sleep(100);
////							}
//							if (rearrangedPolys[2].get(k).equals(posCrit) && rearrangedPolys[0].get(k).equals(tempbufferlist.get(j))){
//								notUsed=false;
//							}
//						}
						
						
						if (actdist<minDistance && notUsed){
							minDistance=actdist;
							polyID=tempbufferlist.get(j);
						}	
						
					}
					
					int locationremove=-1;
					int j=0;
					boolean notfound=true;
					while (j<numberlocations && notfound==true){
						if (j!=posCrit){
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
					
					for (int k=0;k<rearrangedPolys[0].size();k++){
						if (rearrangedPolys[2].get(k).equals(posCrit) && rearrangedPolys[1].get(k).equals(locationremove) && rearrangedPolys[0].get(k).equals(polyID)){
							counterIdUsed++;
						}
					}
					
					lastArea=posCrit;
					rearrangedPolys[0].add(polyID);
					rearrangedPolys[1].add(locationremove);
					rearrangedPolys[2].add(posCrit);
					
//					if (rearrangedPolys[0].size()>numberlocations){
//						rearrangedPolys[0].remove(0);
//						rearrangedPolys[1].remove(0);
//						rearrangedPolys[2].remove(0);
//					}
					
					System.out.println("Set to "+(posCrit+1)+" remove "+polyID+" from "+(locationremove+1)+","+allocPolys[locationremove].size()+","+allocPolys[i].size());
					geomAllocPolys[locationremove].remove(allocPolys[locationremove].indexOf(polyID));
					geomAllocPolys[posCrit].add(polysGeometry[1].get(polysGeometry[0].indexOf(String.valueOf(polyID))));
					
					allocPolys[locationremove].remove(Integer.valueOf(polyID));
					allocPolys[posCrit].add(polyID);
					
//					System.out.println(allocPolys[locationremove].size()+","+allocPolys[i].size());
					criteriaf=addToCriteria(polyID, posCrit+1, locationremove+1, criteriaf,true,polysGeometry);
					tempbufferlist.clear();
					
					if (counterIdUsed==numberlocations){
						satisfied=true;
						System.out.println("no better rearrangement possible");
					}
					
					notarranged=false;
					
				}
				else no++;
			}
			}
			
			if (no==numberlocations){
				satisfied=true;
			}
		}
		return allocPolys;
	}
	
}
