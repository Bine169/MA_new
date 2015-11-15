package whitespotGreenfield;

import java.util.ArrayList;
import java.util.List;

public class PolygonContainer {
	private List<Polygon> polygons;
	
	public PolygonContainer(){
		polygons = new ArrayList<Polygon>();
	}
	
	public void add(int id, String geometry, double criteria){
		//init Polygon
		Polygon poly = new Polygon();
		poly.setId(id);
		poly.setGeometry(geometry);
		poly.setCriteria(criteria);
		poly.setFlagAllocatedLocation(false);
		
		//set Polygon
		polygons.add(poly);
	}
	
	public double getArea(int id){
		Polygon poly = polygons.get(id);
		return poly.getArea();
	}
	
	public Location getAllocatedLocation(int id){
		Polygon poly = polygons.get(id);
		return poly.getAllocatedLocation();
	}
	
	public Double[] getCentroid(int id){
		Polygon poly = polygons.get(id);
		return poly.getCentroid();
	}
	
	public double getCircumference(int id){
		Polygon poly = polygons.get(id);
		return poly.getCircumference();
	}
	
	public double getCircumferenceShared(int id, int idNeigh){
		Polygon poly = polygons.get(id);
		int pos=-1;
		for (int i=0;i<poly.getNeighbours().size();i++){
			if (poly.getNeighbours().get(i).getId()==idNeigh){
				pos=i;
			}
		}
		
		return poly.getCircumferenceShared(pos);
	}
	
	public double getDistance(int id){
		Polygon poly = polygons.get(id);
		return poly.getDistance();
	}
	
	
	public Boolean getFlagAllocatedLocation(int id){
		Polygon poly = polygons.get(id);
		return poly.getFlagAllocatedLocation();
	}
	
	public Polygon getPolygon(int id){
		return polygons.get(id);
	}
	
	public Polygon getPolygonById(int numberpolygons, int polyID){
		Polygon poly=new Polygon();
		for (int i=0;i<numberpolygons;i++){
			Polygon actPoly=getPolygon(i);
			if (actPoly.getId()==polyID){
				poly=actPoly;
			}
		}
		
		return poly;
	}
	
	public void removeAllocatedLocation(int id){
		Polygon actPoly = getPolygon(id);
		actPoly.removeAllocatedLocation();
		actPoly.setFlagAllocatedLocation(false);
	}
	
	public void setAllocatedLocation(int idPoly, int idLocation, LocationContainer locationContainer){
		Polygon poly = getPolygon(idPoly);
		Location loc = locationContainer.getLocation(idLocation);
		poly.setAllocatedLocation(loc);
		locationContainer.setAllocatedPolygon(poly,idLocation);
		setFlagAllocatedLocation(idPoly, true);
	}
	
	public void setArea(int id, double area){
		Polygon poly = polygons.get(id);
		poly.setArea(area);
	}
	
	public void setCentroid(int idPoly, double lon, double lat){
		Polygon poly=getPolygon(idPoly);
		poly.setCentroid(lon, lat);
	}
	
	public void setCircumference(int id, double circum){
		Polygon poly = polygons.get(id);
		poly.setCircumference(circum);
	}
	
	public void setCircumferenceShared(int id, double circum){
		Polygon poly = polygons.get(id);
		poly.setCircumferenceshared(circum);
	}
	
	public void setDistance(int idPoly, double dist){
		Polygon poly=getPolygon(idPoly);
		poly.setDistance(dist);
	}
	
	private void setFlagAllocatedLocation(int id, boolean flag){
		Polygon poly = polygons.get(id);
		poly.setFlagAllocatedLocation(flag);
	}
	
	public void setNeighbours(int id, List<Polygon> neighbours){
		Polygon poly = getPolygon(id);
		poly.setNeighbours(neighbours);
	}
	
}
