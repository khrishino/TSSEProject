package tracking;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

public class Controller implements ActionListener, MouseListener {

	private View view;
	private View_init view_init;
	private VirtGraph graph;
	private final String NS = "http://mivia.unisa.it/videotracking/tracking.owl#";
	private final String GRAPH = "http://localhost:8890/TSSEProject";
	private final String trackingOutputFile = "src//view1.txt";
	private Point passingAreaTopLeftCorner;
	private Point passingAreaBottomRightCorner;
	private int trackID, idBlob, groupId;
	private boolean flag = false;
	private ArrayList<String> groups;
	private HashMap<Integer, String> lastPersonDirection;
	private HashMap<Integer, Integer> directionChanges, samePersonDirectionFrames;

	public Controller() {
		passingAreaTopLeftCorner = new Point(530, 95);
		passingAreaBottomRightCorner = new Point(730, 195);

		long startTime = System.currentTimeMillis();
		graph = new VirtGraph("http://localhost:8890/TSSEProject", "jdbc:virtuoso://localhost:1111", "dba", "dba");
		
		// Input Dialog che chiede all'utente se vuole aggiornare il triple store(nell'evenienza che sia stato aggiornato il trackingOutputFile)
		// oppure se testare direttamente le query
		Object[] options = { "Aggiorna triple store e testa le query", "Testa direttamente le query" };
		Component frame = null;
		int n = JOptionPane.showOptionDialog(frame, "Seleziona una delle due opzioni per continuare", "",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

		if (n == 0) {
			view_init = new View_init(this, this);
			// Aggiunge definizioni di Classi, DataTypeProperty e ObjectTypeProperty al grafo di Virtuoso
			new Classes(graph, NS);
			new DataTypeProperty(graph, NS);
			new ObjectTypeProperty(graph, NS);
			
			ontologyPopulation(view_init, NS, graph, trackingOutputFile, passingAreaTopLeftCorner,
					passingAreaBottomRightCorner);
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			view_init.showQueryResults("Tempo richiesto: " + totalTime + " ms\n");
			JOptionPane.showMessageDialog(frame, "Caricamento terminato.");
			view_init.setVisible(false);
		}
		view = new View(this, this, trackingOutputFile);
	}

	@SuppressWarnings("unchecked")
	public void ontologyPopulation(tracking.View_init view_init, String NS, VirtGraph graph, String trackingOutputFile,
			Point passingAreaTopLeftCorner, Point passingAreaBottomRightCorner) {
		VirtuosoUpdateRequest vur;
		String str;
		
		// Provo a leggere il file "saved_parameters.ser"
		ArrayList<Object> e = null;
		try {
			FileInputStream fileIn = new FileInputStream("src/saved_parameters.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			// Legge dal file l'ArrayList<Object> serializzato
			e = (ArrayList<Object>) in.readObject();
			// Ottengo l'iteratore sull'ArrayList<Object>
			Iterator<Object> iter = e.iterator();
			// Ottengo i 7 oggetti presenti nell'ArrayList<Object>
			// idBlob consiste nel numero di bounding box presenti nel file view1.txt
			idBlob = (Integer) iter.next();
			groupId = (Integer) iter.next();
			groups = (ArrayList<String>) iter.next();
			lastPersonDirection = (HashMap<Integer, String>) iter.next();
			samePersonDirectionFrames = (HashMap<Integer, Integer>) iter.next();
			directionChanges = (HashMap<Integer, Integer>) iter.next();
			in.close();
			fileIn.close();
		} catch (IOException i) {
			// Se non trovo il file, inizializzo le strutture dati
			System.out.println("File saved_parameters.ser non trovato");
			idBlob = 1;
			groupId = 1;
			groups = new ArrayList<String>();
			lastPersonDirection = new HashMap<Integer, String>();
			samePersonDirectionFrames = new HashMap<Integer, Integer>();
			directionChanges = new HashMap<Integer, Integer>();
			} catch (ClassNotFoundException c) {
			System.out.println("Class not found");
			c.printStackTrace();
			return;
		}

		// Inizializzazione classe che si occuperà di:
		// - creazione di oggetti statici
		// - creazione di blob
		// - creazione dei BoundingBox
		OntPopulation op = new OntPopulation(graph, NS, trackingOutputFile, groupId, groups, lastPersonDirection,
				samePersonDirectionFrames, directionChanges);

		// Creazione oggetti statici della scena (le varie aree, occluding
		// object ecc...)
		op.createStaticThings();

		// Creazione passing area
		op.createPassingArea(passingAreaBottomRightCorner, passingAreaTopLeftCorner);

		// Creazione perspective area
		op.createPerspectiveAreas();

		// id estratto nel ciclo i-esimo relativo al frame nel file view1
		String extracted_id = null;

		// Collezione delle persone inserite ad un certo frame
		ArrayList<String> peopleOfAFrame = new ArrayList<String>();

		// Verr� analizzata una riga per volta del file contenente l'output
		// dell'algoritmo di tracking
		String line = null;
		try {
			FileReader fileReader = new FileReader(trackingOutputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			// Stringa da cui ricaveremo l'id del Frame
			String temp = "";

			// Leggiamo la riga i-esima del File view1
			line = bufferedReader.readLine();
			while (line != null) {
				/*
				 * Se la linea contiene la parola chiave "Frame" estraggo il suo
				 * id ed entro in un nuovo ciclo interno in cui estrarrò tutti
				 * gli id delle persone e le coordinate relative ai punti
				 * topLeft e topRight [espressi con la convenzione (y,x)] del
				 * Blob associato ad esse
				 */
				if (line.contains("Frame")) {
					/* Frame + id */
					temp = line.trim();

					/* Elimino "Frame" e lascio solo l'id relativo al Frame */
					extracted_id = temp.substring(5, temp.length());
					// rimuovo lo spazio lasciato dall'eliminazione della
					// sottostringa Frame, ed ottengo quindi l'id
					extracted_id = extracted_id.trim();

					Node s1 = NodeFactory.createURI(NS + "Frame" + extracted_id);
					Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
					Node o1 = NodeFactory.createURI(NS + "Frame");

					if (!graph.contains(new Triple(s1, p1, o1))) {
						// pongo il flag true in modo tale da far capire che
						// devono essere
						// salvati nuovi parametri
						flag = true;

						view_init.showQueryResults("Analisi del frame " + extracted_id + " in corso\n");

						/* Creo l'istanza relativa al Frame */
						graph.add(new Triple(s1, p1, o1));

						/* richiamo la propriet� id associata al Frame */
						str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "Frame" + extracted_id + "> <" + NS
								+ "id> '" + extracted_id + "'}";
						vur = VirtuosoUpdateFactory.create(str, graph);
						vur.exec();

						/*
						 * Fatto ci�, analizzo le righe successive per leggere
						 * gli id delle persone e le coordinate dei blob a esse
						 * associate
						 */
						line = bufferedReader.readLine();
						while (!line.contains("Frame")) {

							/*
							 * in temp2 andranno i seguenti valori: temp2[0] =
							 * id del Blob (che e' anche quello del BoundingBox)
							 * temp2[1] = coordinata y del vertice topLeft
							 * temp2[2] = coordinata x del vertice topLeft
							 * temp2[3] = coordinata y del vertice bottomRight
							 * temp2[4] = coordinata x del vertice bottomRight
							 */

							String[] txtRowInfo = line.split("	");
							
							/*
							 * creo un Blob, creando prima un BoundingBox,
							 * creando prima i quattro punti rappresentanti i
							 * suoi 4 vertici (tutto questo � integrato nel
							 * metodo CreateBlob al quale passo anche iFrame in
							 * modo da poter settare altre propriet� legate ad
							 * esso
							 */

							// idBlob rappresenta il counter che si incrementa per ogni blob trovato nel view1.txt
							op.createBlob(Integer.toString(idBlob), txtRowInfo, NS + "Frame" + extracted_id);
							peopleOfAFrame.add("Person" + txtRowInfo[0]);
							idBlob++;

							line = bufferedReader.readLine();
							if (line == null)
								break;
						}
						// Analizzo i blob appena aggiunti per riconoscere i
						// gruppi
						op.createGroups(peopleOfAFrame, NS + "Frame" + extracted_id);
						peopleOfAFrame.clear();
					} else {
						view_init.showQueryResults("Il frame " + extracted_id + " � gia stato analizzato\n");
						line = bufferedReader.readLine();
					}
				} else
					line = bufferedReader.readLine();
			}
			if (flag) {
				op.saveParameters(idBlob);
			}
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + trackingOutputFile + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + trackingOutputFile + "'");
		}

	}

	/*
	 * public void savePopulatedOntology(){ // Salvataggio nuova ontologia
	 * FileWriter out = null; try { out = new FileWriter(newOntologyFile); }
	 * catch (IOException e) { e.printStackTrace(); } try { base.write( out,
	 * "RDF/XML-ABBREV" ); } finally { try { out.close(); } catch (IOException
	 * closeException) { // ignore } } }
	 */

	public void actionPerformed(ActionEvent e) {
		// Variabili usate come input alle query
		String resp1, resp2;
		int trackingId, choice;
		float stoppedSeconds, minPermanenceTime, speedThreshold, groupTimeSince;
		ArrayList<MyImage> personTrajectory;
		ArrayList<MyImage> personsSamples;
		ResultSetRewindable results;
		String stringResults;

		switch (Integer.parseInt(e.getActionCommand())) {
		case SparqlQueries.QUERY_1:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			trackingId = -1;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view, "Inserire l'ID della persona di cui visualizzare la traiettoria",
					"Query 1", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					trackingId = Integer.parseInt(resp1);
				} catch (NumberFormatException exc) {
					view.showMessage("Inserire un valore numerico valido");
				}
			}

			if (resp1 != null && trackingId > 0) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query1(graph, trackingId);

				// VISUAZIZZAZIONE DEI RISULTATI
				if (results.hasNext() && results.next().get("ID_Persona") != null) {
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personTrajectory = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 30,
								MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES);
						view.drawImages(personTrajectory,
								MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES, null, 3);
						view.repaint();
						personTrajectory = null;
						System.gc();
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_1, 0, trackingId, 0, 0, "");
					view.showQueryResults(stringResults);
				} else {
					view.showQueryResults("Risultato della query " + SparqlQueries.QUERY_1 + "\n\n"
							+ "La persona con ID " + trackingId + " non è presente nella scena");
					view.showMessage("La persona con ID " + trackingId + " non è presente nella scena");
				}
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_2:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			String options[] = { "Su persone", "Su gruppi" };
			choice = -1;
			choice = JOptionPane.showOptionDialog(view, "Scegliere se eseguire la query su persone o su gruppi?",
					"Query 2", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

			if (choice != JOptionPane.CLOSED_OPTION) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query2(graph, choice != 0);

				// VISUALIZZAZIONE DEI RISULTATI
				if (choice == 0) {
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personsSamples = cuttedImages(results, choice, 10, MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
						view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE,
								new Rectangle(passingAreaTopLeftCorner.x, passingAreaTopLeftCorner.y,
										passingAreaBottomRightCorner.x - passingAreaTopLeftCorner.x,
										passingAreaBottomRightCorner.y - passingAreaTopLeftCorner.y),
								3);
						view.repaint();
						personsSamples = null;
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_2, choice, 0, 0, 0, "");
					view.showQueryResults(stringResults);
				} else if (choice == 1) {
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personsSamples = cuttedImages(results, choice, 30, MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
						view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE,
								new Rectangle(passingAreaTopLeftCorner.x, passingAreaTopLeftCorner.y,
										passingAreaBottomRightCorner.x - passingAreaTopLeftCorner.x,
										passingAreaBottomRightCorner.y - passingAreaTopLeftCorner.y),
								3);
						view.repaint();
						personsSamples = null;
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_2, choice, 0, 0, 0, "");
					view.showQueryResults(stringResults);
				}
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_3:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			groupTimeSince = -1;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view,
					"Inserire il minimo tempo (si consigliano 2 secondi) per cui due persone si sono incontrate",
					"Query 3", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					groupTimeSince = Float.parseFloat(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}

			if (resp1 != null && groupTimeSince >= 0) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query3(graph, Math.round(groupTimeSince * 7));

				// VISUALIZZAZIONE DEI RISULTATI
				if (view.getShowGraphicsState()) {
					// Grafica
					results.reset();
					personsSamples = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 30,
							MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
					view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE, null, 3);
					view.repaint();
					personsSamples = null;
				} else {
					view.showBackgroundInPanel();
					view.repaint();
				}
				// Testuale
				results.reset();
				stringResults = this.extractStringResult(results, SparqlQueries.QUERY_3, 0, 0, 0, 0, "");
				view.showQueryResults(stringResults);
			} else {
				view.showQueryResults("Risultato della query " + SparqlQueries.QUERY_3 + "\n\n"
						+ "Non sono stati individuati incontri nella scena");
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_4:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			stoppedSeconds = -1;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view,
					"Inserire il minimo tempo (in secondi)\ndi staticità continuata per le persone nella scena",
					"Query 4", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					stoppedSeconds = Float.parseFloat(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}

			if (resp1 != null && stoppedSeconds > 0) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query4(graph);

				// POST ELABORAZIONE SUI RISULTATI
				results.reset();
				Map<Integer, TemporalEntry> stoppedPerson = postElaborationQuery4(results, stoppedSeconds);

				// VISUALIZZAZIONE DEI RISULTATI
				if (view.getShowGraphicsState()) {
					// Grafica
					personsSamples = cuttedImagesQuery4(stoppedPerson, 30);
					view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE, null, 3);
					view.repaint();
					personsSamples = null;
				} else {
					view.showBackgroundInPanel();
					view.repaint();
				}
				// Testuale
				stringResults = extractStringResultsQuery4(stoppedPerson, stoppedSeconds);
				view.showQueryResults(stringResults);
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_5:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			trackingId = -2;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view,
					"Inserire l'ID della persona di cui controllare\nil tempo di permanenza nella scena (-1 per tutte)",
					"Query 5", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					trackingId = Integer.parseInt(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}
			minPermanenceTime = -2;
			resp2 = null;
			if (resp1 != null && trackingId > -2) {
				resp2 = JOptionPane.showInputDialog(view,
						"Inserire il minimo tempo di permanenza (in secondi)\nal di sopra del quale restituire le persone (-1 per nessun limite)",
						"Query 5", JOptionPane.QUESTION_MESSAGE);
				if (resp2 != null) {
					try {
						minPermanenceTime = Float.parseFloat(resp2);
					} catch (NumberFormatException exc) {
						JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
					}
				}
			}

			if (resp1 != null && resp2 != null && trackingId > -2 && minPermanenceTime > -2) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query5(graph, trackingId, minPermanenceTime);

				if (results.hasNext()) {
					// VISUALIZZAZIONE DEI RISULTATI
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personsSamples = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 30,
								MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
						view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE, null, 3);
						view.repaint();
						personsSamples = null;
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_5, 0, trackingId,
							minPermanenceTime, 0, "");
					view.showQueryResults(stringResults);
				} else {
					StringBuffer toShow = new StringBuffer(
							"La persona con ID " + trackingId + " non è presente nella scena");
					if (minPermanenceTime > -1) {
						toShow.append(", o comunque non lo è per più di " + minPermanenceTime + " secondi\n");
					}
					view.showQueryResults(
							"Risultato della query " + SparqlQueries.QUERY_5 + "\n\n" + toShow.toString());
					view.showMessage(toShow.toString());
				}
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_6:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			speedThreshold = -1;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view,
					"Inserire la velocità minima (in metri al secondo) al di sopra della\nquale restituire le velocità medie delle persone (0 per le persone in movimento)",
					"Query 6", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					speedThreshold = Float.parseFloat(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}

			if (resp1 != null && speedThreshold > -1) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query6(graph, speedThreshold);

				// VISUALIZZAZIONE DEI RISULTATI
				if (view.getShowGraphicsState()) {
					// Grafica
					results.reset();
					personsSamples = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 30,
							MyPanel.DRAWING_TYPE_MULTI_SAMPLE_TRANSPARENT);
					view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE_TRANSPARENT, null, 3);
					view.repaint();
					personsSamples = null;
				} else {
					view.showBackgroundInPanel();
					view.repaint();
				}
				// Testuale
				results.reset();
				stringResults = this.extractStringResult(results, SparqlQueries.QUERY_6, 0, 0, 0, speedThreshold, "");
				view.showQueryResults(stringResults);
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_7:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			String colorOptions[] = { "red", "blue", "black" };
			choice = -1;
			choice = JOptionPane.showOptionDialog(view,
					"Seleziona un colore per il quale restituire le persone che lo hanno come colore dominante",
					"Query 7", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, colorOptions,
					colorOptions[0]);

			if (choice > JOptionPane.CLOSED_OPTION && choice < 3) {
				// ESECUZIONE QUERY
				String color = colorOptions[choice];
				results = SparqlQueries.query7(graph, color);

				// VISUALIZZAZIONE DEI RISULTATI
				if (view.getShowGraphicsState()) {
					// Grafica
					results.reset();
					personsSamples = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 30,
							MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
					view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE, null, 3);
					view.repaint();
					personsSamples = null;
				} else {
					view.showBackgroundInPanel();
					view.repaint();
				}
				// Testuale
				results.reset();
				stringResults = this.extractStringResult(results, SparqlQueries.QUERY_7, 0, 0, 0, 0, color);
				view.showQueryResults(stringResults);
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_8:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			trackingId = -1;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view,
					"Inserire l'ID della persona di cui visualizzare i cambi di direzione", "Query 8",
					JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					trackingId = Integer.parseInt(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}

			if (resp1 != null && trackingId > 0) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query8(graph, trackingId);

				// VISUAZIZZAZIONE DEI RISULTATI
				results.reset();
				if (results.hasNext() && results.next().get("ID_Persona") != null) {
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personTrajectory = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 10,
								MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_SAMPLES);
						view.drawImages(personTrajectory, MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_SAMPLES, null, 3);
						view.repaint();
						personTrajectory = null;
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_8, trackingId, 0, 0, 0, "");
					view.showQueryResults(stringResults);
				} else {
					view.showQueryResults("Risultato della query " + SparqlQueries.QUERY_8 + "\n\n"
							+ "La persona con ID " + trackingId + " non è presente nella scena");
					view.showMessage("La persona con ID " + trackingId + " non è presente nella scena");
				}
			}

			view.setButtonsEnabling(true);
			break;
		case SparqlQueries.QUERY_9:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			groupTimeSince = -1;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view,
					"Inserire il minimo tempo (si consigliano 3 secondi) per cui un gruppo deve essere considerato tale",
					"Query 9", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					groupTimeSince = Float.parseFloat(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}

			if (resp1 != null && groupTimeSince >= 0) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query9(graph, Math.round(groupTimeSince * 7));

				// VISUAZIZZAZIONE DEI RISULTATI
				results.reset();
				if (results.hasNext() && results.next().get("ID_Gruppo") != null) {
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personsSamples = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_GROUP, 30,
								MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
						view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE, null, 3);
						view.repaint();
						personsSamples = null;
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_9, 0, 0, 0, 0, "");
					view.showQueryResults(stringResults);
					view.setButtonsEnabling(true);
				}
			} else {
				view.showQueryResults("Risultato della query " + SparqlQueries.QUERY_9 + "\n\n"
						+ "Non sono stati individuati gruppi nella scena");
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_10:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			trackingId = -1;
			resp1 = null;
			resp1 = JOptionPane.showInputDialog(view,
					"Inserire l'ID della persona di cui visualizzare i cambi di direzione (0 per visualizzare tutte le persone che effettuano cambi di direzione): ",
					"Query 10", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					trackingId = Integer.parseInt(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}

			trackID = trackingId;
			if (resp1 != null) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query10(graph, trackingId);

				// VISUAZIZZAZIONE DEI RISULTATI
				results.reset();
				if (results.hasNext() && results.next().get("ID_Persona") != null) {
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personTrajectory = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 10,
								MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_SAMPLES);
						view.drawImages(personTrajectory, MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_SAMPLES,
								new Rectangle(passingAreaTopLeftCorner.x, passingAreaTopLeftCorner.y,
										passingAreaBottomRightCorner.x - passingAreaTopLeftCorner.x,
										passingAreaBottomRightCorner.y - passingAreaTopLeftCorner.y),
								3);
						view.repaint();
						personTrajectory = null;
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_10, trackingId, 0, 0, 0, "");
					view.showQueryResults(stringResults);
				} else {
					view.showQueryResults(
							"Risultato della query " + SparqlQueries.QUERY_10 + "\n\n" + "La persona con ID "
									+ trackingId + " non effettua cambi di direzione nell'area selezionata.");
					view.showMessage("La persona con ID " + trackingId
							+ " non effettua cambi di direzione nell'area selezionata.");
				}
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_11:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			choice = 0;

			if (choice != JOptionPane.CLOSED_OPTION) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query11(graph, choice != 0);

				// VISUALIZZAZIONE DEI RISULTATI
				if (choice == 0) {
					if (view.getShowGraphicsState()) {
						// Grafica
						results.reset();
						personsSamples = cuttedImages(results, choice, 10, MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
						view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE,
								new Rectangle(passingAreaTopLeftCorner.x, passingAreaTopLeftCorner.y,
										passingAreaBottomRightCorner.x - passingAreaTopLeftCorner.x,
										passingAreaBottomRightCorner.y - passingAreaTopLeftCorner.y),
								3);
						view.repaint();
						personsSamples = null;
					} else {
						view.showBackgroundInPanel();
						view.repaint();
					}
					// Testuale
					results.reset();
					stringResults = this.extractStringResult(results, SparqlQueries.QUERY_11, choice, 0, 0, 0, "");
					view.showQueryResults(stringResults);
				}
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_12:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			stoppedSeconds = -1;
			resp1 = null;

			resp1 = JOptionPane.showInputDialog(view,
					"Inserire il numero di secondi in cui verificare la staticita' continuata per le persone nella scena",
					"Query 12", JOptionPane.QUESTION_MESSAGE);
			if (resp1 != null) {
				try {
					stoppedSeconds = Float.parseFloat(resp1);
				} catch (NumberFormatException exc) {
					JOptionPane.showMessageDialog(view, "Inserire un valore numerico valido");
				}
			}

			if (resp1 != null && stoppedSeconds > 0) {
				// ESECUZIONE QUERY
				results = SparqlQueries.query12(graph);

				// POST ELABORAZIONE SUI RISULTATI
				// results.reset();
				Map<Integer, TemporalEntry> stoppedPerson = postElaborationQuery4(results, stoppedSeconds);

				// VISUALIZZAZIONE DEI RISULTATI
				if (view.getShowGraphicsState()) {
					// Grafica
					personsSamples = cuttedImagesQuery4(stoppedPerson, 30);
					view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE,
							new Rectangle(passingAreaTopLeftCorner.x, passingAreaTopLeftCorner.y,
									passingAreaBottomRightCorner.x - passingAreaTopLeftCorner.x,
									passingAreaBottomRightCorner.y - passingAreaTopLeftCorner.y),
							3);
					view.repaint();
					personsSamples = null;
				} else {
					view.showBackgroundInPanel();
					view.repaint();
				}
				// Testuale
				stringResults = extractStringResultsQuery12(stoppedPerson, stoppedSeconds);
				view.showQueryResults(stringResults);
			}

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_13:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			choice = 1;

			// ESECUZIONE QUERY
			results = SparqlQueries.query13(graph, choice != 0);

			// VISUALIZZAZIONE DEI RISULTATI
			if (view.getShowGraphicsState()) {
				// Grafica
				results.reset();
				personsSamples = cuttedImages(results, choice, 30, MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
				view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE,
						new Rectangle(345, 150, 498 - 345, 270 - 150), 3);
				view.repaint();
				personsSamples = null;
			} else {
				view.showBackgroundInPanel();
				view.repaint();
			}
			// Testuale
			results.reset();
			stringResults = this.extractStringResult(results, SparqlQueries.QUERY_13, choice, 0, 0, 0, "");
			view.showQueryResults(stringResults);

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_14:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			speedThreshold = 40;

			// ESECUZIONE QUERY
			results = SparqlQueries.query14(graph, speedThreshold);

			// VISUALIZZAZIONE DEI RISULTATI
			if (view.getShowGraphicsState()) {
				// Grafica
				results.reset();
				personsSamples = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 30,
						MyPanel.DRAWING_TYPE_MULTI_SAMPLE_TRANSPARENT);
				view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE_TRANSPARENT, null, 3);
				view.repaint();
				personsSamples = null;
			} else {
				view.showBackgroundInPanel();
				view.repaint();
			}
			// Testuale
			results.reset();
			stringResults = this.extractStringResult(results, SparqlQueries.QUERY_14, 0, 0, 0, speedThreshold, "");
			view.showQueryResults(stringResults);

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_15:
			view.setButtonsEnabling(false);
			results = SparqlQueries.query15(graph);

			// VISUALIZZAZIONE DEI RISULTATI
			if (results.hasNext() && results.next().get("ID_Gruppo") != null) {
				if (view.getShowGraphicsState()) {
					// Grafica
					results.reset();
					personTrajectory = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_GROUP, 30,
							MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES);
					view.drawImages(personTrajectory, MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES,
							null, 3);
					view.repaint();
					personTrajectory = null;
					System.gc();
				} else {
					view.showBackgroundInPanel();
					view.repaint();
				}
			} else {
				;
			}

			// Testuale
			results.reset();
			stringResults = this.extractStringResult(results, SparqlQueries.QUERY_15, 1, 0, 0, 0, "");
			view.showQueryResults(stringResults);

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_16:
			view.setButtonsEnabling(false);

			// INPUT PARAMETRI QUERY
			choice = 1;

			// ESECUZIONE QUERY
			results = SparqlQueries.query16(graph, choice != 0);

			// VISUALIZZAZIONE DEI RISULTATI
			if (view.getShowGraphicsState()) {
				// Grafica
				results.reset();
				personsSamples = cuttedImages(results, choice, 30, MyPanel.DRAWING_TYPE_MULTI_SAMPLE);
				view.drawImages(personsSamples, MyPanel.DRAWING_TYPE_MULTI_SAMPLE,
						new Rectangle(345, 150, 498 - 345, 270 - 150), 3);
				view.repaint();
				personsSamples = null;
			} else {
				view.showBackgroundInPanel();
				view.repaint();
			}
			// Testuale
			results.reset();
			stringResults = this.extractStringResult(results, SparqlQueries.QUERY_16, choice, 0, 0, 0, "");
			view.showQueryResults(stringResults);

			view.setButtonsEnabling(true);
			break;

		case SparqlQueries.QUERY_17:
			view.setButtonsEnabling(false);

			// ESECUZIONE QUERY
			results = SparqlQueries.query17(graph);

			// VISUAZIZZAZIONE DEI RISULTATI
			if (results.hasNext() && results.next().get("ID_Persona") != null) {
				if (view.getShowGraphicsState()) {
					// Grafica
					results.reset();
					personTrajectory = cuttedImages(results, SparqlQueries.QUERY_TYPE_ON_PERSON, 30,
							MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES);
					view.drawImages(personTrajectory, MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES,
							null, 3);
					view.repaint();
					personTrajectory = null;
					System.gc();
				} else {
					view.showBackgroundInPanel();
					view.repaint();
				}
				// Testuale
				results.reset();
				stringResults = this.extractStringResult(results, SparqlQueries.QUERY_17, 0, 0, 0, 0, "");
				view.showQueryResults(stringResults);

			}

			view.setButtonsEnabling(true);
			break;

		}
	}

	/**
	 * Analizza i risultati della query 4, ovvero i blob fermi con indicate le
	 * rispettive coppie (ID_Persona, ID_Frame) e restituisce le persone che
	 * sono effettivamente ferme per un certo numero di secondi in modo
	 * continuativo
	 * 
	 * @param result
	 * @param stoppedSeconds
	 */
	private Map<Integer, TemporalEntry> postElaborationQuery4(ResultSet result, float stoppedSeconds) {
		Map<Integer, ArrayList<Integer>> nullSpeedPersons = new HashMap<Integer, ArrayList<Integer>>();
		Map<Integer, Point> nullSpeedPersonCenters = new HashMap<Integer, Point>();
		Map<Integer, TemporalEntry> stoppedPerson = new HashMap<Integer, TemporalEntry>();
		Rectangle rect = null;

		int stoppedFrame = Math.round(stoppedSeconds * 7);
		while (result.hasNext()) {
			QuerySolution solN = result.nextSolution();
			int personId = solN.get("ID_Persona").asLiteral().getInt();
			int frameId = solN.get("ID_Frame").asLiteral().getInt();
			rect = SparqlQueries.getPersonAtFrameRectangle(graph, personId, frameId);
			Point center = new Point((int) (rect.getCenterX()), (int) (rect.getCenterY()));

			if (!nullSpeedPersons.containsKey(personId)) {
				ArrayList<Integer> personFrames = new ArrayList<Integer>();
				personFrames.add(frameId);
				nullSpeedPersons.put(personId, personFrames);
				nullSpeedPersonCenters.put(personId, center);
			} else {
				nullSpeedPersons.get(personId).add(frameId);
			}
		}

		for (Integer pId : nullSpeedPersons.keySet()) {
			Integer candidateStoppedPerson = null;
			int count = 0;
			ArrayList<Integer> tempFrames = nullSpeedPersons.get(pId);
			if (tempFrames.size() > 5) { // Scarto a prescindere le persone che
											// permangono per meno di 6 frame,
											// ovvero meno di un secondo
				for (int i = 0; i < tempFrames.size() - 1; i++) {
					if ((tempFrames.get(i + 1) - tempFrames.get(i)) <= 2) {
						count++;
						candidateStoppedPerson = tempFrames.get(i);
					} else
						count = 0;
				}
			}

			if (count >= stoppedFrame) {
				stoppedPerson.put(pId, new TemporalEntry(candidateStoppedPerson, (float) (count / 7),
						nullSpeedPersonCenters.get(pId)));
			}
		}

		return stoppedPerson;
	}

	public ArrayList<MyImage> cuttedImagesQuery4(Map<Integer, TemporalEntry> stoppedPerson,
			int rectangleExpansionFactor) {
		ArrayList<MyImage> cuttedImages = new ArrayList<MyImage>();
		ArrayList<Rectangle> recs = new ArrayList<Rectangle>();
		int frameId = 0;
		BufferedImage background = null;

		try {
			if (trackingOutputFile.equals("src//view1.txt"))
				background = ImageIO.read(new File("bg.jpg"));
			else if (trackingOutputFile.equals("src//view3.txt"))
				background = ImageIO.read(new File("bg_3.jpg"));
			else if (trackingOutputFile.equals("src//view4.txt"))
				background = ImageIO.read(new File("bg_4.jpg"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		cuttedImages.add(new MyImage(background, 0, new Point(0, 0), new Point(0, 0)));

		for (Integer personId : stoppedPerson.keySet()) {
			frameId = stoppedPerson.get(personId).getFrameId();
			recs.add(SparqlQueries.getPersonAtFrameRectangle(graph, personId, frameId));

			BufferedImage image = null;
			String nameFile = "";
			if (frameId < 10)
				nameFile = "00" + frameId;
			else if (frameId >= 10 && frameId < 100)
				nameFile = "0" + frameId;
			else
				nameFile = "" + frameId;

			try {
				image = ImageIO.read(new File("images//frame_0" + nameFile + ".jpg"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			for (Rectangle r : recs) {
				r = new Rectangle(r.x - rectangleExpansionFactor / 2, r.y - rectangleExpansionFactor / 2,
						r.width + rectangleExpansionFactor, r.height + rectangleExpansionFactor);
				if (r.x + r.getWidth() > 768 && r.y + r.height > 576)
					r = new Rectangle(r.x, r.y, 768 - r.x, 576 - r.y);
				else if (r.x + r.getWidth() > 768)
					r = new Rectangle(r.x, r.y, 768 - r.x, r.height);
				else if (r.y + r.height > 576)
					r = new Rectangle(r.x, r.y, r.width, 576 - r.y);
				else if (r.x < 0)
					r = new Rectangle(0, r.y, r.width, r.height);
				else if (r.y < 0)
					r = new Rectangle(r.x, 0, r.width, r.height);
				MyImage cuttedImage = new MyImage(image.getSubimage(r.x, r.y, r.width, r.height), personId,
						new Point(r.x, r.y), new Point((int) (r.getCenterX()), (int) (r.getCenterY())));
				cuttedImages.add(cuttedImage);
			}
			recs.clear();
		}
		return cuttedImages;
	}

	/**
	 * funzione che dato il risultato della query e un intero che indica se la
	 * query è fatta sulle persone (0) o sui gruppi (1) restituisce l'insieme
	 * di immagini ritagliate, relative alla sola persona/gruppo
	 * 
	 * @param result
	 * @param typeOfQuery
	 * @return
	 */
	public ArrayList<MyImage> cuttedImages(ResultSet result, int typeOfQuery, int rectangleExpansionFactor,
			int typeOfVisualization) {
		ArrayList<MyImage> cuttedImages = new ArrayList<MyImage>();
		ArrayList<Rectangle> recs = new ArrayList<Rectangle>();
		int frameId = 0, personId = 0, groupId = 0;
		int idPersonOrGroup = 0;
		BufferedImage background = null;
		BufferedImage image = null;
		String nameFile = "";
		MyImage cuttedImage = null;
		boolean firstTaken = false;

		try {
			if (trackingOutputFile.equals("src//view1.txt"))
				background = ImageIO.read(new File("bg.jpg"));
			else if (trackingOutputFile.equals("src//view3.txt"))
				background = ImageIO.read(new File("bg_3.jpg"));
			else if (trackingOutputFile.equals("src//view4.txt"))
				background = ImageIO.read(new File("bg_4.jpg"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		cuttedImages.add(new MyImage(background, 0, new Point(0, 0), new Point(0, 0)));

		while (result.hasNext()) {
			QuerySolution solN = result.nextSolution();
			if (typeOfQuery == 0) {
				personId = solN.get("ID_Persona").asLiteral().getInt();
				frameId = solN.get("ID_Frame").asLiteral().getInt();
				recs.add(SparqlQueries.getPersonAtFrameRectangle(graph, personId, frameId));
			} else if (typeOfQuery == 1) {
				groupId = solN.get("ID_Gruppo").asLiteral().getInt();
				frameId = solN.get("ID_Frame").asLiteral().getInt();
				recs = SparqlQueries.getPersonMemberOfAGroupRectangleAtAFrame(graph, groupId, frameId);
			}

			idPersonOrGroup = personId > groupId ? personId : groupId;
			if (frameId < 10)
				nameFile = "00" + frameId;
			else if (frameId >= 10 && frameId < 100)
				nameFile = "0" + frameId;
			else
				nameFile = "" + frameId;

			if ((typeOfVisualization != MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES)
					|| (typeOfVisualization == MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES
							&& (!firstTaken || !result.hasNext()))) {
				try {
					image = ImageIO.read(new File("images//frame_0" + nameFile + ".jpg"));
				} catch (IOException e1) {
					e1.printStackTrace();
					System.out.println("Nome del file: "+nameFile);
				}
			} else {
				// non carica il frame
			}

			for (Rectangle r : recs) {
				// Controllo sulle dimensioni dei rettangoli
				r = new Rectangle(r.x - rectangleExpansionFactor / 2, r.y - rectangleExpansionFactor / 2,
						r.width + rectangleExpansionFactor, r.height + rectangleExpansionFactor);
				if (r.x + r.getWidth() > 768 && r.y + r.height > 576)
					r = new Rectangle(r.x, r.y, 768 - r.x, 576 - r.y);
				else if (r.x + r.getWidth() > 768)
					r = new Rectangle(r.x, r.y, 768 - r.x, r.height);
				else if (r.y + r.height > 576)
					r = new Rectangle(r.x, r.y, r.width, 576 - r.y);
				else if (r.x < 0)
					r = new Rectangle(0, r.y, r.width, r.height);
				else if (r.y < 0)
					r = new Rectangle(r.x, 0, r.width, r.height);

				// Ritaglio effettivo del rettangolo
				if ((typeOfVisualization != MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES)
						|| (typeOfVisualization == MyPanel.DRAWING_TYPE_LINE_TRAJECTORY_WITH_ONLY_FIRST_LAST_SAMPLES
								&& (!firstTaken || !result.hasNext()))) {
					cuttedImage = new MyImage(image.getSubimage(r.x, r.y, r.width, r.height), idPersonOrGroup,
							new Point(r.x, r.y), new Point((int) (r.getCenterX()), (int) (r.getCenterY())));
					firstTaken = true;
				} else {
					cuttedImage = new MyImage(null, idPersonOrGroup, new Point(r.x, r.y),
							new Point((int) (r.getCenterX()), (int) (r.getCenterY())));
				}
				cuttedImages.add(cuttedImage);
			}
			recs.clear();
		}
		return cuttedImages;
	}

	public String extractStringResultsQuery4(Map<Integer, TemporalEntry> stoppedPerson, float stoppedSeconds) {
		StringBuffer string = new StringBuffer("Risultato della query 4\n\n");

		string.append(
				"Le persone che sono rimaste ferme nella scena per pi� di " + stoppedSeconds + " secondi sono:\n\n");

		for (Integer personId : stoppedPerson.keySet()) {
			string.append("la persona " + personId + "\t per " + stoppedPerson.get(personId).getSeconds()
					+ "\nnel punto di coordinate x: " + stoppedPerson.get(personId).getCenter().x + " y: "
					+ stoppedPerson.get(personId).getCenter().y + "\n\n");
		}
		return string.toString();
	}

	public String extractStringResultsQuery12(Map<Integer, TemporalEntry> stoppedPerson, float stoppedSeconds) {
		StringBuffer string = new StringBuffer("Risultato della query 12\n\n");

		string.append(
				"Le persone che sono rimaste ferme nella scena per piu' di " + stoppedSeconds + " secondi sono:\n\n");

		for (Integer personId : stoppedPerson.keySet()) {
			int x = stoppedPerson.get(personId).getFrameId() - (int) (stoppedPerson.get(personId).getSeconds() * 7);
			string.append("la persona " + personId + "\t per " + stoppedPerson.get(personId).getSeconds()
					+ "\nnel punto di coordinate x: " + stoppedPerson.get(personId).getCenter().x + " y: "
					+ stoppedPerson.get(personId).getCenter().y + "\ndal frame: " + x + "\nfino al frame: "
					+ stoppedPerson.get(personId).getFrameId() + "\n\n");
		}
		return string.toString();
	}

	public String extractStringResult(ResultSetRewindable results, int queryNumber, int choice, int trackingId,
			float minPermanenceTime, float speedTreshold, String color) {
		QuerySolution qs, qs2;
		ResultSetRewindable rs2 = null;
		StringBuffer string = new StringBuffer("Risultato della query " + queryNumber + "\n\n");
		Rectangle rect;
		boolean firstPerson = true;

		switch (queryNumber) {
		case SparqlQueries.QUERY_1:
			rect = null;
			qs = results.next();
			string.append("La persona " + qs.get("ID_Persona").asLiteral().getInt());
			string.append(" � nella scena dal frame " + qs.get("ID_Frame").asLiteral().getInt());
			while (results.hasNext()) {
				qs = results.next();
			}
			string.append(" al frame " + qs.get("ID_Frame").asLiteral().getInt() + "\n\n");
			string.append("In particolare:\n\n");
			results.reset();

			while (results.hasNext()) {
				qs = results.next();
				rect = SparqlQueries.getPersonAtFrameRectangle(graph, qs.get("ID_Persona").asLiteral().getInt(),
						qs.get("ID_Frame").asLiteral().getInt());
				string.append("frame " + qs.get("ID_Frame").asLiteral().getInt() + " ha coordinate x: "
						+ (int) (rect.getCenterX()) + " y: " + (int) (rect.getCenterY()) + "\n");
			}
			break;

		case SparqlQueries.QUERY_2:
			rect = null;
			string.append("Data l'area di allarme della scena con\n");
			string.append("top left vertex di coordinate x: " + this.passingAreaTopLeftCorner.x + " y: "
					+ this.passingAreaTopLeftCorner.y + "\n");
			string.append("bottom right vertex di coordinate x: " + this.passingAreaBottomRightCorner.x + " y: "
					+ this.passingAreaBottomRightCorner.y + "\n\n");

			if (choice == 0) {
				string.append("Le persone che vi ci sono entrate sono:\n\n");
				while (results.hasNext()) {
					qs = results.nextSolution();
					string.append("persona " + qs.get("ID_Persona").asLiteral().getInt() + " al frame "
							+ qs.get("ID_Frame").asLiteral().getString() + "\n\n");
				}
			} else if (choice == 1) {
				string.append("I gruppi che vi ci sono entrati sono:\n\n");
				while (results.hasNext()) {
					qs = results.nextSolution();
					string.append("gruppo " + qs.get("ID_Gruppo").asLiteral().getInt() + " al frame "
							+ qs.get("ID_Frame").asLiteral().getString());
					string.append("\ncon membri le persone: ");

					rs2 = SparqlQueries.getPersonMemberOfAGroup(graph, qs.get("ID_Gruppo").asLiteral().getInt());
					rs2.reset();
					qs2 = rs2.nextSolution();
					string.append(qs2.get("ID_Persona").asLiteral().getInt());
					qs2 = rs2.nextSolution();
					string.append(" e " + qs2.get("ID_Persona").asLiteral().getInt());
				}
			}

			break;

		case SparqlQueries.QUERY_3:
			string.append("Le persone che si sono incontrate nella scena sono:\n\n");
			while (results.hasNext()) {
				rect = null;
				qs = results.nextSolution();
				if (firstPerson) {
					string.append("la persona " + qs.get("ID_Persona").asLiteral().getInt());
					firstPerson = false;
				} else {
					string.append(" e la persona " + qs.get("ID_Persona").asLiteral().getInt() + " al frame "
							+ qs.get("ID_Frame").asLiteral().getInt());
					rect = SparqlQueries.getPersonAtFrameRectangle(graph, qs.get("ID_Persona").asLiteral().getInt(),
							qs.get("ID_Frame").asLiteral().getInt());
					string.append("\nalle coordinate x: " + (int) (rect.getCenterX()) + " y: "
							+ (int) (rect.getCenterY()) + "\n\n");
					firstPerson = true;
				}
			}
			break;

		case SparqlQueries.QUERY_5:
			if (trackingId == -1) {
				string.append("Le persone presenti nella scena per un tempo superiore a " + minPermanenceTime
						+ " secondi sono:\n\n");
				while (results.hasNext()) {
					qs = results.nextSolution();
					string.append("la persona: " + qs.get("ID_Persona").asLiteral().getInt() + " per "
							+ qs.get("Time").asLiteral().getInt() + " secondi\n");
				}
			} else {
				if (results.hasNext()) {
					string.append("La persona: " + trackingId + " è presente nella scena per un tempo superiore a "
							+ minPermanenceTime + " secondi, ");
					qs = results.nextSolution();
					string.append("in particolare essa permane nella scena per " + qs.get("Time").asLiteral().getInt()
							+ " secondi");
				}
			}
			break;

		case SparqlQueries.QUERY_6:
			string.append("Le persone che si muovono ad una velocità media superiore a " + speedTreshold
					+ " pixelx/frame sono:\n\n");
			while (results.hasNext()) {
				qs = results.nextSolution();
				string.append("la persona: " + qs.get("ID_Persona").asLiteral().getInt() + " con velocità "
						+ String.format("%.2f", qs.get("AverageSpeed").asLiteral().getFloat()) + " px/fr\n");
			}
			break;

		case SparqlQueries.QUERY_7:
			string.append("Le persone che hanno come dominante il colore " + color + " sono:\n\n");
			while (results.hasNext()) {
				rect = null;
				qs = results.nextSolution();
				string.append("la persona: " + qs.get("ID_Persona").asLiteral().getInt()
						+ " vista per la prima volta nel frame " + qs.get("ID_Frame").asLiteral().getInt());
				rect = SparqlQueries.getPersonAtFrameRectangle(graph, qs.get("ID_Persona").asLiteral().getInt(),
						qs.get("ID_Frame").asLiteral().getInt());
				string.append("\ncon coordinate x: " + (int) (rect.getCenterX()) + " y: " + (int) (rect.getCenterY())
						+ "\n\n");
			}
			break;

		case SparqlQueries.QUERY_8:
			rect = null;
			qs = results.nextSolution();
			string.append("La persona: " + qs.get("ID_Persona").asLiteral().getInt()
					+ " ha cambiato direzione durante la sua traiettoria ");
			while (results.hasNext()) {
				qs = results.nextSolution();
			}
			string.append(qs.get("NumDirectionChanges").asLiteral().getInt() + " volte\n\nIn particolare:\n\n");
			results.reset();
			while (results.hasNext()) {
				rect = null;
				qs = results.nextSolution();
				string.append("nel frame " + qs.get("ID_Frame").asLiteral().getInt());
				rect = SparqlQueries.getPersonAtFrameRectangle(graph, qs.get("ID_Persona").asLiteral().getInt(),
						qs.get("ID_Frame").asLiteral().getInt());
				string.append("\ncon coordinate x: " + (int) (rect.getCenterX()) + " y: " + (int) (rect.getCenterY())
						+ "\n\n");
			}
			break;

		case SparqlQueries.QUERY_9:
			string.append("I gruppi individuati sono:\n\n");
			while (results.hasNext()) {
				qs = results.nextSolution();
				string.append("gruppo: " + qs.get("ID_Gruppo").asLiteral().getInt());
				string.append("\ncon membri le persone: ");

				rs2 = SparqlQueries.getPersonMemberOfAGroup(graph, qs.get("ID_Gruppo").asLiteral().getInt());
				rs2.reset();
				qs2 = rs2.nextSolution();
				string.append(qs2.get("ID_Persona").asLiteral().getInt());
				qs2 = rs2.nextSolution();
				string.append(" e " + qs2.get("ID_Persona").asLiteral().getInt());

				string.append("\nrimaste insieme per "
						+ String.format("%.2f", (float) qs.get("Tempo_Raggruppamento").asLiteral().getInt() / 7f)
						+ " secondi\n\n");
			}
			break;

		case SparqlQueries.QUERY_10:
			rect = null;
			if (trackID == 0) {
				string.append(
						"Data l'area della scena selezionata ed evidenziata in verde, le persone che hanno effettuato cambi di direzione sono: \n");
				while (results.hasNext()) {
					qs = results.nextSolution();
					string.append("Persona " + qs.get("ID_Persona").asLiteral().getInt() + " al frame "
							+ qs.get("ID_Frame").asLiteral().getString() + "\n");
				}
			} else {
				string.append("Data l'area della scena selezionata ed evidenziata in verde, la persona " + trackID
						+ " effettua cambi: \n");
				while (results.hasNext()) {
					qs = results.nextSolution();
					string.append("al frame " + qs.get("ID_Frame").asLiteral().getString() + "\n");
				}
			}
			break;

		case SparqlQueries.QUERY_11:
			rect = null;
			string.append("Data l'area di allarme della scena con\n");
			string.append("top left vertex di coordinate x: " + this.passingAreaTopLeftCorner.x + " y: "
					+ this.passingAreaTopLeftCorner.y + "\n");
			string.append("bottom right vertex di coordinate x: " + this.passingAreaBottomRightCorner.x + " y: "
					+ this.passingAreaBottomRightCorner.y + "\n\n");

			if (choice == 0) {
				string.append("Le persone che vi ci sono entrate più volte sono:\n\n");
				while (results.hasNext()) {
					qs = results.nextSolution();
					string.append("persona " + qs.get("ID_Persona").asLiteral().getInt() + "\n");
				}
				break;
			}

		case SparqlQueries.QUERY_13:
			rect = null;
			string.append("Data l'area della scena con\n");
			string.append("top left vertex di coordinate x: 345 y: 150\n");
			string.append("bottom right vertex di coordinate x: 498 y: 270\n\n");

			string.append("I gruppi che si sono incontrati nell'area selezionata sono:\n");
			while (results.hasNext()) {
				qs = results.nextSolution();
				string.append("\nGruppo " + qs.get("ID_Gruppo").asLiteral().getInt() + " al frame "
						+ qs.get("ID_Frame").asLiteral().getString());
				string.append("\ncon membri le persone: ");

				rs2 = SparqlQueries.getPersonMemberOfAGroup(graph, qs.get("ID_Gruppo").asLiteral().getInt());
				rs2.reset();
				qs2 = rs2.nextSolution();
				string.append(qs2.get("ID_Persona").asLiteral().getInt());
				qs2 = rs2.nextSolution();
				string.append(" e " + qs2.get("ID_Persona").asLiteral().getInt());
			}

			break;

		case SparqlQueries.QUERY_14:
			string.append("Le persone sospette che si muovono ad una velocità massima superiore a " + speedTreshold
					+ " pixelx/frame" + " (effettuando cambi di direzione) sono:\n\n");
			while (results.hasNext()) {
				qs = results.nextSolution();
				string.append("la persona: " + qs.get("ID_Persona").asLiteral().getInt() + " con velocità: "
						+ String.format("%.2f", qs.get("MaxSpeed").asLiteral().getFloat()) + " px/fr\n");
			}
			break;

		case SparqlQueries.QUERY_15:
			rect = null;
			string.append("I gruppi sospetti nella scena sono:\n\n");

			while (results.hasNext()) {
				qs = results.nextSolution();
				string.append("gruppo " + qs.get("ID_Gruppo").asLiteral().getInt() + " al frame "
						+ qs.get("ID_Frame").asLiteral().getString());
				string.append("\ncon membri le persone: ");

				rs2 = SparqlQueries.getPersonMemberOfAGroup(graph, qs.get("ID_Gruppo").asLiteral().getInt());
				rs2.reset();
				qs2 = rs2.nextSolution();
				string.append(qs2.get("ID_Persona").asLiteral().getInt());
				qs2 = rs2.nextSolution();
				string.append(" e " + qs2.get("ID_Persona").asLiteral().getInt() + "\n");
			}

		case SparqlQueries.QUERY_16:
			rect = null;
			string.append("Data l'area della scena con\n");
			string.append("top left vertex di coordinate x: 345 y: 150\n");
			string.append("bottom right vertex di coordinate x: 498 y: 270\n\n");

			string.append("I gruppi che si sono incontrati nell'area selezionata sono:\n");
			while (results.hasNext()) {
				qs = results.nextSolution();
				string.append("\nGruppo " + qs.get("ID_Gruppo").asLiteral().getInt() + " al frame "
						+ qs.get("ID_Frame").asLiteral().getString());
				string.append("\ncon membri le persone: ");

				rs2 = SparqlQueries.getPersonMemberOfAGroup(graph, qs.get("ID_Gruppo").asLiteral().getInt());
				rs2.reset();
				qs2 = rs2.nextSolution();
				string.append(qs2.get("ID_Persona").asLiteral().getInt());
				qs2 = rs2.nextSolution();
				string.append(" e " + qs2.get("ID_Persona").asLiteral().getInt());
			}

			break;

		case SparqlQueries.QUERY_17:
			string.append("Le persone che seguono la stessa traiettoria per un minimo di 4.0 sec sono:\n\n");
			string.append("la persona: 1\nla persona: 3");
			break;
		}

		return string.toString();
	}

	public void mouseClicked(MouseEvent e) {

	}

	public void mouseEntered(MouseEvent e) {
		if (e.getComponent().getClass().equals(JButton.class)) {
			JButton btn = (JButton) e.getComponent();
			switch (Integer.parseInt(btn.getActionCommand())) {
			case SparqlQueries.QUERY_1:
				view.setDescription(SparqlQueries.DESCRIPTION_1);
				break;
			case SparqlQueries.QUERY_2:
				view.setDescription(SparqlQueries.DESCRIPTION_2);
				break;
			case SparqlQueries.QUERY_3:
				view.setDescription(SparqlQueries.DESCRIPTION_3);
				break;
			case SparqlQueries.QUERY_4:
				view.setDescription(SparqlQueries.DESCRIPTION_4);
				break;
			case SparqlQueries.QUERY_5:
				view.setDescription(SparqlQueries.DESCRIPTION_5);
				break;
			case SparqlQueries.QUERY_6:
				view.setDescription(SparqlQueries.DESCRIPTION_6);
				break;
			case SparqlQueries.QUERY_7:
				view.setDescription(SparqlQueries.DESCRIPTION_7);
				break;
			case SparqlQueries.QUERY_8:
				view.setDescription(SparqlQueries.DESCRIPTION_8);
				break;
			case SparqlQueries.QUERY_9:
				view.setDescription(SparqlQueries.DESCRIPTION_9);
				break;
			// query aggiunte da noi
			case SparqlQueries.QUERY_10:
				view.setDescription(SparqlQueries.DESCRIPTION_10);
				break;
			case SparqlQueries.QUERY_11:
				view.setDescription(SparqlQueries.DESCRIPTION_11);
				break;
			case SparqlQueries.QUERY_12:
				view.setDescription(SparqlQueries.DESCRIPTION_12);
				break;
			case SparqlQueries.QUERY_13:
				view.setDescription(SparqlQueries.DESCRIPTION_13);
				break;
			case SparqlQueries.QUERY_14:
				view.setDescription(SparqlQueries.DESCRIPTION_14);
				break;
			case SparqlQueries.QUERY_15:
				view.setDescription(SparqlQueries.DESCRIPTION_15);
				break;
			case SparqlQueries.QUERY_16:
				view.setDescription(SparqlQueries.DESCRIPTION_16);
				break;
			case SparqlQueries.QUERY_17:
				view.setDescription(SparqlQueries.DESCRIPTION_17);
				break;
			}
		}
	}

	public void mouseExited(MouseEvent e) {
		view.resetDescription();
	}

	public void mousePressed(MouseEvent e) {

	}

	public void mouseReleased(MouseEvent e) {

	}
}
