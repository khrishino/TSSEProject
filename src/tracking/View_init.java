package tracking;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.TextArea;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
public class View_init extends JFrame {
	private static final long serialVersionUID = 1L;
	private int pos=1;
	
	private JPanel centerPanel = null;
	private JPanel rightPanel = null;
	private TextArea taResults = null;
	private String stringResults, infoFrame;

	public View_init(ActionListener actionListener, MouseListener mouseListener){
		super("Semantic Enterprise Systems - Project 3 ");
		this.setLayout(new BorderLayout());
		
		centerPanel = new JPanel(new BorderLayout());
		this.add(centerPanel, BorderLayout.CENTER);
		rightPanel = new JPanel(new BorderLayout());
		rightPanel.setPreferredSize(new Dimension(400,630));
		this.add(rightPanel, BorderLayout.EAST);
		
		stringResults = "Qui verranno visualizzati i risultati delle query";
		taResults = new TextArea(stringResults,50,100,TextArea.SCROLLBARS_VERTICAL_ONLY);
		taResults.setFont(new Font("Arial", Font.PLAIN, 20));
		rightPanel.add(taResults, BorderLayout.CENTER);
        
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.pack();
        this.setVisible(true);
	}

	public void showMessage(String message){
		JOptionPane.showMessageDialog(this, message);
	}
	
	public void showQueryResults(String stringResults){
		this.infoFrame = stringResults;
		taResults.insert(infoFrame, pos);
		pos--;
	}
	
	public void setDescription(String queryDescription){
		taResults.setText(queryDescription);
	}

	public void resetDescription(){
		taResults.setText(stringResults);
	}
}