package whitespotGreenfield;

import java.util.ArrayList;
import java.util.List;

public class LocationContainer {
	public List<Location> locations;
	
	public LocationContainer(){
		locations = new ArrayList<Location>();
	}
	
	public void add(int id, double lon, double lat){
		//init location
		Location loc = new Location();
		loc.setId(id);
		loc.setLon(lon);
		loc.setLat(lat);
		loc.setCriteria(0.0);
		loc.setWeightValue(-1.0);
		
		//set Location to Container
		locations.add(loc);
		System.out.println("set Location "+id);
	}
	
	public void add(int id){
		add(id,-1,-1);
	}
	
	public List<Polygon> getAllocatedPolygon(int id){
		Location loc = locations.get(id);
		return loc.getAllocatedPolygon();
	}
	
	public double getCompactness(int id){
		Location loc = locations.get(id);
		return loc.getWeightValue();
	}
	
	public double getCriteria(int id){
		Location loc = locations.get(id);
		return loc.getCriteria();
	}
	
	public Location getLocation(int id){
		return locations.get(id);
	}
	
	public Location getLocationByID(int id){
		Location loc=null;
		
		for (int i=0;i<locations.size();i++){
			Location actLoc = locations.get(i);
			if (actLoc.getId()==id){
				loc=actLoc;
			}
		}
		
		return loc;
	}
	
	public void resetAllocatedPolys(int id){
		Location loc = getLocationByID(id);
		loc.resetAllocatedPolys();
	}
	
	public void setAllocatedPolygon(Polygon poly, int idLocation){
		Location loc = getLocation(idLocation);
		loc.setAllocatedPolygon(poly);
	}
	
	public void setCriteria(int id, double crit){
		Location loc = getLocation(id);
		loc.setCriteria(crit);
	}
	
	public void setCriteriaByLoc(Location loc, double crit){
		loc.setCriteria(crit);
	}
	
	public void setHomePoly(int idLoc, int idPoly){
		Location loc = getLocation(idLoc);
		loc.setHomePoly(idPoly);
	}
	
	public void setLonLat (double lon, double lat, int id){
		Location loc = getLocation(id);
		loc.setLat(lat);
		loc.setLon(lon);
	}
	
	public void setWeightValue(int id, double weight){
		Location loc = getLocation(id);
		loc.setWeightValue(weight);
	}
	
	public void setWeightValuebyLoc(Location loc, double weight){
		loc.setWeightValue(weight);
	}
	
}
