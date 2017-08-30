package tracking;

import java.awt.Point;

public class Functions {

	private static final int STOPPED_CENTER_RANGE_X = 3;
	private static final int STOPPED_CENTER_RANGE_Y = 3;

	/**
	 * Dati TopLeft e BottomRight,ritorna un Point con le coordinate del centro
	 * 
	 * @param TopLeft
	 * @param BottomRight
	 * @return
	 */
	public static Point getCenter(Point TopLeft, Point BottomRight) {
		return new Point((BottomRight.x + TopLeft.x) / 2, (TopLeft.y + BottomRight.y) / 2);
	}

	/**
	 * Dati TopLeft e BottomRight,calcola le coordinate degli altri due punti
	 * 
	 * @param TopLeft
	 * @param BottomRight
	 * @param TopRight
	 * @param BottomLeft
	 */
	public static void computeVertexs(Point TopLeft, Point BottomRight, Point TopRight, Point BottomLeft) {
		TopRight.x = BottomRight.x;
		TopRight.y = TopLeft.y;
		BottomLeft.x = TopLeft.x;
		BottomLeft.y = BottomRight.y;
	}

	/**
	 * 
	 * @param CenterPre
	 * @param CenterPost
	 * @return
	 */

	public static String getDirection(Point CenterPre, Point CenterPost) {

		String direction = "";

		double d_x = Math.abs(CenterPost.getX() - CenterPre.getX());
		double d_y = Math.abs(CenterPost.getY() - CenterPre.getY());
		double delta_x = (CenterPost.getX() - CenterPre.getX());
		double delta_y = (CenterPost.getY() - CenterPre.getY());

		if (d_x <= STOPPED_CENTER_RANGE_X && d_y <= STOPPED_CENTER_RANGE_Y)
			direction = "stopped";
		else {
			// Calc the angle IN RADIANS using the atan2
			double theta = Math.atan2(delta_y, delta_x);

			// this.angle is now in degrees
			// or leave off *180/Math.PI if you want radians
			double angle = theta * 180 / Math.PI;
			if (angle == 180)
				direction = "west";
			if (angle == 0)
				direction = "est";
			if (angle == 90)
				direction = "south";
			if (angle == -90)
				direction = "nord";
			if (angle > -90 && angle < 0)
				direction = "nest";
			if (angle > 0 && angle < 90)
				direction = "sest";
			if (angle > 90 && angle < 180)
				direction = "swest";
			if (angle > -180 && angle < -90)
				direction = "nwest";
		}
		return direction;

	}

	/*
	 * Per il calcolo della velocità è stato fatto questo ragionamento: - siccome il
	 * FrameRate è di 7 Frame per secondo, vuol dire che tra un Frame e l'altro
	 * passa un tempo pari a circa 0,14 secondi (1/7); - per la distanza invece,
	 * bisogna tener conto che essa deve essere espressa in metri, mentre tale
	 * distanza è ottenuta in pixel, pertanto, sapendo che un Metro è uguale a circa
	 * 3779 pixels, posso calcolare la distanza in metri attraverso la formula
	 * inversa; Pertanto, dividendo la distanza rispetto al tempo, è possibile
	 * ottenere la velocità
	 */
	public static float Speed(Point CenterPre, Point CenterPost) {
		// Distanza in pixel
		float distance = (float) (CenterPost.distance(CenterPre));
		float distanceInMeters = distance / 3779;

		float speed = (float) (distanceInMeters / 0.14);
		return speed;
	}

	public static float Time(float time) {
		return time + 0.14f;
	}

}