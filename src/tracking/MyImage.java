package tracking;

import java.awt.*;

public class MyImage {
	private Point sourcePoint;
	private Point centerPoint;
	private Image image;
	private int idPersonOnImage;
	
	public MyImage(Image image,int idPersonOnImage, Point sourcePoint, Point centerPoint){
		this.image = image;
		this.sourcePoint = sourcePoint;
		this.centerPoint = centerPoint;
		this.idPersonOnImage = idPersonOnImage;
	}
	public int getIdPersonOnImage(){
		return idPersonOnImage;
	}

	public Point getSourcePoint() {
		return sourcePoint;
	}

	public void setSourcePoint(Point sourcePoint) {
		this.sourcePoint = sourcePoint;
	}

	public Point getCenterPoint() {
		return centerPoint;
	}

	public void setCenterPoint(Point centerPoint) {
		this.centerPoint = centerPoint;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}
}
