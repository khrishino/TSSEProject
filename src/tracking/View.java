package tracking;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class View extends JFrame {
	private static final long serialVersionUID = 1L;
	public static final String QUERY_1 = "1";
	public static final String QUERY_2 = "2";
	public static final String QUERY_3 = "3";
	public static final String QUERY_4 = "4";
	public static final String QUERY_5 = "5";
	public static final String QUERY_6 = "6";
	public static final String QUERY_7 = "7";
	public static final String QUERY_8 = "8";
	public static final String QUERY_9 = "9";
	public static final String QUERY_10 = "10";
	public static final String QUERY_11 = "11";
	public static final String QUERY_12 = "12";
	public static final String QUERY_13 = "13";
	public static final String QUERY_14 = "14";
	public static final String QUERY_15 = "15";
	public static final String QUERY_16 = "16";
	public static final String QUERY_17 = "17";
	
	private JPanel centerPanel = null;
	private JPanel rightPanel = null;
	private MyPanel pnImage = null;
	private JCheckBox ckShowGraphics = null;
	private JPanel pnButton = null;
	private TextArea taResults = null;
	private JButton btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn10, btn11, btn12, btn13, btn14, btn15, btn16, btn17;
	private String stringResults, trackingOutputFile;

	public View(ActionListener actionListener, MouseListener mouseListener, String trackingOutputFile){
		super("Semantic Enterprise Systems - Project 3 ");
		this.setLayout(new BorderLayout());
		
		this.trackingOutputFile = trackingOutputFile;
		
		centerPanel = new JPanel(new BorderLayout());
		this.add(centerPanel, BorderLayout.CENTER);
		rightPanel = new JPanel(new BorderLayout());
		rightPanel.setPreferredSize(new Dimension(400,630));
		this.add(rightPanel, BorderLayout.EAST);
		
		pnButton = new JPanel(new GridLayout(9,1));
		pnButton.setSize(new Dimension(100, 576));
		centerPanel.add(pnButton, BorderLayout.EAST);
		
		btn1 = new JButton("Query 1");
		btn1.setFont(new Font("Arial", Font.PLAIN, 14));
		btn1.setActionCommand(QUERY_1);
		btn1.addActionListener(actionListener);
		btn1.addMouseListener(mouseListener);
		pnButton.add(btn1);
		
		btn2 = new JButton("Query 2");
		btn2.setFont(new Font("Arial", Font.PLAIN, 14));
		btn2.setActionCommand(QUERY_2);
		btn2.addActionListener(actionListener);
		btn2.addMouseListener(mouseListener);
		pnButton.add(btn2);
		
		btn3 = new JButton("Query 3");
		btn3.setFont(new Font("Arial", Font.PLAIN, 14));
		btn3.setActionCommand(QUERY_3);
		btn3.addActionListener(actionListener);
		btn3.addMouseListener(mouseListener);
		pnButton.add(btn3);
		
		btn4 = new JButton("Query 4");
		btn4.setFont(new Font("Arial", Font.PLAIN, 14));
		btn4.setActionCommand(QUERY_4);
		btn4.addActionListener(actionListener);
		btn4.addMouseListener(mouseListener);
		pnButton.add(btn4);
		
		btn5 = new JButton("Query 5");
		btn5.setFont(new Font("Arial", Font.PLAIN, 14));
		btn5.setActionCommand(QUERY_5);
		btn5.addActionListener(actionListener);
		btn5.addMouseListener(mouseListener);
		pnButton.add(btn5);
		
		btn6 = new JButton("Query 6");
		btn6.setFont(new Font("Arial", Font.PLAIN, 14));
		btn6.setActionCommand(QUERY_6);
		btn6.addActionListener(actionListener);
		btn6.addMouseListener(mouseListener);
		pnButton.add(btn6);
		
		btn7 = new JButton("Query 7");
		btn7.setFont(new Font("Arial", Font.PLAIN, 14));
		btn7.setActionCommand(QUERY_7);
		btn7.addActionListener(actionListener);
		btn7.addMouseListener(mouseListener);
		pnButton.add(btn7);
		
		btn8 = new JButton("Query 8");
		btn8.setFont(new Font("Arial", Font.PLAIN, 14));
		btn8.setActionCommand(QUERY_8);
		btn8.addActionListener(actionListener);
		btn8.addMouseListener(mouseListener);
		pnButton.add(btn8);
		
		btn9 = new JButton("Query 9");
		btn9.setFont(new Font("Arial", Font.PLAIN, 14));
		btn9.setActionCommand(QUERY_9);
		btn9.addActionListener(actionListener);
		btn9.addMouseListener(mouseListener);
		pnButton.add(btn9);
		
		btn10 = new JButton("Query 10");
		btn10.setFont(new Font("Arial", Font.PLAIN, 14));
		btn10.setActionCommand(QUERY_10);
		btn10.addActionListener(actionListener);
		btn10.addMouseListener(mouseListener);
		//btn9.setSize(new Dimension(90,40));
		pnButton.add(btn10);
		
		btn11 = new JButton("Query 11");
		btn11.setFont(new Font("Arial", Font.PLAIN, 14));
		btn11.setActionCommand(QUERY_11);
		btn11.addActionListener(actionListener);
		btn11.addMouseListener(mouseListener);
		pnButton.add(btn11);
		
		btn12 = new JButton("Query 12");
		btn12.setFont(new Font("Arial", Font.PLAIN, 14));
		btn12.setActionCommand(QUERY_12);
		btn12.addActionListener(actionListener);
		btn12.addMouseListener(mouseListener);
		pnButton.add(btn12);
		
		btn13 = new JButton("Query 13");
		btn13.setFont(new Font("Arial", Font.PLAIN, 14));
		btn13.setActionCommand(QUERY_13);
		btn13.addActionListener(actionListener);
		btn13.addMouseListener(mouseListener);
		pnButton.add(btn13);
		
		btn14 = new JButton("Query 14");
		btn14.setFont(new Font("Arial", Font.PLAIN, 14));
		btn14.setActionCommand(QUERY_14);
		btn14.addActionListener(actionListener);
		btn14.addMouseListener(mouseListener);
		pnButton.add(btn14);
		
		btn15 = new JButton("Query 15");
		btn15.setFont(new Font("Arial", Font.PLAIN, 14));
		btn15.setActionCommand(QUERY_15);
		btn15.addActionListener(actionListener);
		btn15.addMouseListener(mouseListener);
		pnButton.add(btn15);
		
		btn16 = new JButton("Query 16");
		btn16.setFont(new Font("Arial", Font.PLAIN, 14));
		btn16.setActionCommand(QUERY_16);
		btn16.addActionListener(actionListener);
		btn16.addMouseListener(mouseListener);
		pnButton.add(btn16);
		
		btn17 = new JButton("Query 17");
		btn17.setFont(new Font("Arial", Font.PLAIN, 14));
		btn17.setActionCommand(QUERY_17);
		btn17.addActionListener(actionListener);
		btn17.addMouseListener(mouseListener);
		pnButton.add(btn17);
		
		ckShowGraphics = new JCheckBox("Abilita risultati grafici");
		ckShowGraphics.setSelected(true);
		ckShowGraphics.setFont(new Font("Arial", Font.PLAIN, 20));
		rightPanel.add(ckShowGraphics, BorderLayout.NORTH);
		
		showBackgroundInPanel();
		
		stringResults = "Qui verranno visualizzati i risultati delle query";
		taResults = new TextArea(stringResults,50,100,TextArea.SCROLLBARS_VERTICAL_ONLY);
		taResults.setFont(new Font("Arial", Font.PLAIN, 20));
		rightPanel.add(taResults, BorderLayout.CENTER);
        
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.pack();
        this.setVisible(true);
	}
	
	public boolean getShowGraphicsState() {
		return ckShowGraphics.isSelected();
	}
	
	public void showBackgroundInPanel(){
		BufferedImage image = null;
		try {
			if(trackingOutputFile.equals("src//view1.txt"))
				image = ImageIO.read(new File("bg.jpg"));
			else if(trackingOutputFile.equals("src//view3.txt"))
				image = ImageIO.read(new File("bg_3.jpg"));
			else if(trackingOutputFile.equals("src//view4.txt"))
				image = ImageIO.read(new File("bg_4.jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		ArrayList<MyImage> back = new ArrayList<MyImage>();
		back.add(new MyImage(image,0, new Point(0,0), new Point(0,0)));
		if (pnImage!=null) {
			centerPanel.remove(pnImage);
		}
		pnImage = new MyPanel(back, MyPanel.DRAWING_TYPE_SHADOW_TRAJECTORY, null, 1);
		pnImage.setPreferredSize(new Dimension(768, 576));
		centerPanel.add(pnImage, BorderLayout.CENTER);
		this.pack();
		this.setVisible(true);
	}
	
	public void setButtonsEnabling(boolean enablingStatus){
		btn1.setEnabled(enablingStatus);
		btn2.setEnabled(enablingStatus);
		btn3.setEnabled(enablingStatus);
		btn4.setEnabled(enablingStatus);
		btn5.setEnabled(enablingStatus);
		btn6.setEnabled(enablingStatus);
		btn7.setEnabled(enablingStatus);
		btn8.setEnabled(enablingStatus);
		btn9.setEnabled(enablingStatus);
		btn10.setEnabled(enablingStatus);
		btn11.setEnabled(enablingStatus);
		btn12.setEnabled(enablingStatus);
		btn13.setEnabled(enablingStatus);
		btn14.setEnabled(enablingStatus);
		btn15.setEnabled(enablingStatus);
		btn16.setEnabled(enablingStatus);
		btn17.setEnabled(enablingStatus);
	}
	
	public void drawImages(ArrayList<MyImage> images, int drawingType, Rectangle toDrawRectangle, float borderThickness){
		centerPanel.remove(pnImage);
		pnImage = new MyPanel(images, drawingType, toDrawRectangle, borderThickness);
		pnImage.setPreferredSize(new Dimension(768, 576));
		centerPanel.add(pnImage,BorderLayout.CENTER);
		this.pack();
		this.setVisible(true);
	}
	
	public void showMessage(String message){
		JOptionPane.showMessageDialog(this, message);
	}
	
	public void showQueryResults(String stringResults){
		this.stringResults = stringResults;
		taResults.setText(stringResults);
	}
	
	public void setDescription(String queryDescription){
		taResults.setText(queryDescription);
	}

	public void resetDescription(){
		taResults.setText(stringResults);
	}
}