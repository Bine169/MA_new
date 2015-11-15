package whitespotGreenfield;

import java.util.ArrayList;
import java.util.List;

public class Location {
	private int id;
	private Double lon;
	private Double lat;
	private Integer idHomePoly;
	private List<Polygon> allocatedPolygon=new ArrayList<Polygon>();
	private Double criteria;
	private Double weightValue;
	
	public List<Polygon> getAllocatedPolygon(){
		return this.allocatedPolygon;
	}
	
	public double getCriteria(){
		return this.criteria;
	}
	
	public int getHomePolyId(){
		return this.idHomePoly;
	}
	
	public int getId(){
		return this.id;
	}
	
	public double getLat(){
		return this.lat;
	}
	
	public double getLon(){
		return this.lon;
	}
	
	public double getWeightValue(){
		return this.weightValue;
	}

	
	public void removeAllocatedPolygon(Polygon poly){
		List<Polygon> allocatedPolys = this.allocatedPolygon;
		for (int i=0;i<allocatedPolys.size();i++){
			Polygon actPoly = allocatedPolys.get(i);
			if (actPoly.getId()==poly.getId()){
				this.allocatedPolygon.remove(i);
			}
		}
	}
	
	public void resetAllocatedPolys(){
		this.allocatedPolygon = new ArrayList<Polygon>();
	}
	
	public void setAllocatedPolygon(Polygon poly){
		this.allocatedPolygon.add(poly);
	}
	
	public void setId(int id){
		this.id=id;
	}
	
	public void setCriteria(double crit){
		this.criteria=crit;
	}
	
	public void setHomePoly(int idPoly){
		this.idHomePoly=idPoly;
	}
	
	public void setLon(double lon){
		this.lon=lon;
	}
	
	public void setLat(double lat){
		this.lat=lat;
	}
	
	public void setWeightValue(double weight){
		this.weightValue=weight;
	}
}
