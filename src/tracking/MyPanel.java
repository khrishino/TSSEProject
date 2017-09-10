package tracking;

import java.util.ArrayList;
import java.util.Random;

import javax.swing.*;
import java.awt.*;

public class MyPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	public static final int DRAWING_TYPE_SHADOW_TRAJECTORY = 0;
	public static final int DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES = 1;
	public static final int DRAWING_TYPE_LINE_TRAJECTORY_WITH_SAMPLES = 2;
	public static final int DRAWING_TYPE_MULTI_SAMPLE = 3;
	public static final int DRAWING_TYPE_MULTI_SAMPLE_TRANSPARENT = 4;
	
	private ArrayList<MyImage> images;
	private int drawingType;
	private Rectangle toDrawRectangle;
	private float borderThickness;
	
	public MyPanel(ArrayList<MyImage> images, int drawingType, Rectangle toDrawRectangle, float borderThickness){
		this.images = images;
		this.drawingType = drawingType;
		this.toDrawRectangle = toDrawRectangle;
		this.borderThickness = borderThickness;
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int i;
		float alpha = 1;
		
		Graphics2D g2 = (Graphics2D) g;
		
		// Disegno background
		g2.drawImage(images.get(0).getImage(), images.get(0).getSourcePoint().x, images.get(0).getSourcePoint().y, null);
		
		// Disegno campioni immagini a seconda del tipo di visualizzazione richiesto
		if (drawingType == DRAWING_TYPE_SHADOW_TRAJECTORY){
			for(i = 1;i<images.size()-1;i++){
				MyImage im = images.get(i);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.5f));
				g2.drawImage(im.getImage(), im.getSourcePoint().x, im.getSourcePoint().y, null);
			}
			if (images.size()>1) {
				Stroke oldStroke = g2.getStroke();
				g2.setStroke(new BasicStroke(borderThickness));
				g2.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1f));
				g2.drawImage(images.get(i).getImage(),images.get(i).getSourcePoint().x, images.get(i).getSourcePoint().y, null);
				Random rnd = new Random();
				g2.setColor(new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()));
				g2.drawRect(images.get(i).getSourcePoint().x, images.get(i).getSourcePoint().y, images.get(i).getImage().getWidth(null), images.get(i).getImage().getHeight(null));
				g2.setStroke(oldStroke);
				
			}
		} else if (drawingType == DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES || drawingType == DRAWING_TYPE_LINE_TRAJECTORY_WITH_SAMPLES) {
			Stroke oldStroke = g2.getStroke();
			g2.setStroke(new BasicStroke(borderThickness));
			g2.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.5f));
			if (drawingType == DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES) {
				g2.drawImage(images.get(1).getImage(),images.get(1).getSourcePoint().x, images.get(1).getSourcePoint().y, null);
				g2.drawImage(images.get(images.size()-1).getImage(),images.get(images.size()-1).getSourcePoint().x, images.get(images.size()-1).getSourcePoint().y, null);
			} else if (drawingType == DRAWING_TYPE_LINE_TRAJECTORY_WITH_SAMPLES){
				for(i = 1;i<images.size();i++){
					g2.drawImage(images.get(i).getImage(),images.get(i).getSourcePoint().x, images.get(i).getSourcePoint().y, null);
				}
			}
			g2.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1f));
			Random rnd = new Random();
			Color color1 = new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
			Color color2 = new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
			for(i = 1; i < images.size()-1; i++){
				if(i%2==0)
					g2.setColor(color1);
				else
					g2.setColor(color2);
				g2.drawLine(images.get(i).getCenterPoint().x, images.get(i).getCenterPoint().y,
						    images.get(i+1).getCenterPoint().x, images.get(i+1).getCenterPoint().y);
			}
			g2.setStroke(oldStroke);
			
		} else {
			if (drawingType == DRAWING_TYPE_MULTI_SAMPLE) {
				alpha = 0.5f;
			} else if (drawingType == DRAWING_TYPE_MULTI_SAMPLE_TRANSPARENT) {
				alpha = 0.5f;
			}
			for(i = 1;i<images.size();i++){
				MyImage im = images.get(i);
				Stroke oldStroke = g2.getStroke();
				g2.setStroke(new BasicStroke(borderThickness));
				g2.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha));
				g2.drawImage(im.getImage(),im.getSourcePoint().x,im.getSourcePoint().y, null);
				Random rnd = new Random();
				g2.setColor(new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()));
				g2.drawRect(im.getSourcePoint().x, im.getSourcePoint().y, im.getImage().getWidth(null), images.get(i).getImage().getHeight(null));
				g2.setStroke(oldStroke);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1));
				g2.setFont(new Font("arial",Font.BOLD,16));
				g2.setColor(Color.WHITE);
				g2.drawString(""+im.getIdPersonOnImage(), im.getSourcePoint().x, im.getSourcePoint().y+im.getImage().getHeight(null));
			}
		}
		
		// Disegno passing area se c'è
		if (toDrawRectangle!=null) {
			Stroke oldStroke = g2.getStroke();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.3f));
			g2.setStroke(new BasicStroke(borderThickness));
			g2.setColor(new Color(0, 255, 6));
			g2.fillRect(toDrawRectangle.x, toDrawRectangle.y, toDrawRectangle.width, toDrawRectangle.height);
			g2.setStroke(oldStroke);
		}
		g2.dispose();
	}
}

