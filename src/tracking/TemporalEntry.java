package tracking;

import java.awt.Point;

public class TemporalEntry {
	private Integer frameId;
	private Float seconds;
	private Point center;

	public TemporalEntry(Integer frameId, Float seconds, Point center) {
		this.frameId = frameId;
		this.seconds = seconds;
		this.center = center;
	}

	public Integer getFrameId() {
		return frameId;
	}

	public void setFrameId(Integer frameId) {
		this.frameId = frameId;
	}

	public Float getSeconds() {
		return seconds;
	}

	public void setSeconds(Float seconds) {
		this.seconds = seconds;
	}

	public Point getCenter() {
		return center;
	}

	public void setCenter(Point center) {
		this.center = center;
	}
}
