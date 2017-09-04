package tracking;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

public class OntPopulation {
	private VirtGraph graph;
	private final String GRAPH = "http://localhost:8890/TSSEProject";
	private String NS, trackingOutputFile;
	private ArrayList<String> groups;
	private int groupId;

	// L'ultima direzione delle persone (idPersona, ultimaDirezione)
	private Map<Integer, String> lastPersonDirection;
	// Memorizza il numero di frame della stessa direzione delle persone (idPersona, numFramesStessaDirezione)
	private Map<Integer, Integer> samePersonDirectionFrames;
	// Memorizza il numero di cambi di direzione delle persone (idPersona, cambiamentiDirezione)
	private Map<Integer, Integer> directionChanges;
	// Memorizza il numero di cambi di direzione delle persone (arrivati a 8 si incrementa di 1 il valore in directionChanges)
	private Map<Integer, Integer> newPersonDirection;
	boolean directionChanged;

	private final int yGropuThreshold = 25;
	private final int xGropuThreshold = 105;

	private final float[] hFactor = { 0.40f, 0.44f, 0.68f, 1.0f };
	private final float[] wFactor = { 0.12f, 0.31f, 0.59f, 1.0f };

	// private final float[] hFactor = {0.54f, 0.58f, 0.82f, 1.0f};
	// private final float[] wFactor = {0.26f, 0.45f, 0.73f, 1.0f};

	private final int[] minHPerspArea = { 20, 35, 64, 90 };
	private final int[] minWPerspArea = { 10, 11, 15, 19 };
	private final int[] maxHPerspArea = { 50, 85, 125, 165 };
	private final int[] maxWPerspArea = { 40, 41, 60, 70 };
	private final int oldGroupThreshold = 14;

	/*
	 * L'utilità di tale struttura dati è spiegata all'inizio del metodo
	 * "CreateStaticThings"
	 */
	ArrayList<Rectangle> rect;

	/* Costruttore */
	public OntPopulation(VirtGraph graph, String NS, String trackingOutputFile, int groupId, ArrayList<String> groups,
			HashMap<Integer, String> lastPersonDirection, HashMap<Integer, Integer> samePersonDirectionFrames,
			HashMap<Integer, Integer> directionChanges) {
		this.graph = graph;
		this.NS = NS;
		this.trackingOutputFile = trackingOutputFile;
		this.newPersonDirection = new HashMap<Integer, Integer>();
		directionChanged = false;

		this.groupId = groupId;
		this.groups = groups;
		this.lastPersonDirection = lastPersonDirection;
		this.samePersonDirectionFrames = samePersonDirectionFrames;
		this.directionChanges = directionChanges;
	}

	public void createPassingArea(Point bottomRight, Point topLeft) {
		VirtuosoUpdateRequest vur;

		Point topRight = new Point(bottomRight.x, topLeft.y);
		Point bottomLeft = new Point(topLeft.x, bottomRight.y);

		// Creazione istanza bbPassingArea1 del tipo BoundingBox
		Node s1 = NodeFactory.createURI(NS + "bbPassingArea1");
		Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o1 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s1, p1, o1));

		setBoundingBox(NS + "bbPassingArea1", "pa1", topLeft, topRight, bottomRight, bottomLeft);

		// Creazione istanza PassingArea1 del tipo PassingArea e aggiunta delle sue
		// properties
		Node s2 = NodeFactory.createURI(NS + "PassingArea1");
		Node p2 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o2 = NodeFactory.createURI(NS + "PassingArea");
		graph.add(new Triple(s2, p2, o2));

		String str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PassingArea1> <" + NS + "id> '08'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s3 = NodeFactory.createURI(NS + "PassingArea1");
		Node p3 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o3 = NodeFactory.createURI(NS + "bbPassingArea1");
		graph.add(new Triple(s3, p3, o3));

		/*
		 * Definisco un rettangolo che tornerà  utile per il controllo dell'intersezione
		 * di un BoundingBox con una specifia area
		 */
		int width = topRight.x - topLeft.x;
		int height = bottomLeft.y - topLeft.y;
		Rectangle rect_passing1 = new Rectangle(topLeft.x, topLeft.y, width, height);
		rect.add(rect_passing1);

	}

	public void createSinglePerspectiveArea(int id, Point tLV, Point bRV, float wF, float hF, int minH, int minW,
			int maxH, int maxW) {
		VirtuosoUpdateRequest vur;

		// Calcolo i restanti due vertici del bounding box
		Point tRV = new Point(bRV.x, tLV.y);
		Point bLV = new Point(tLV.x, bRV.y);

		// Creo l'individuo bounding box e gli setto i vertici
		Node s1 = NodeFactory.createURI(NS + "bbPerspectiveArea" + id);
		Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o1 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s1, p1, o1));

		setBoundingBox(NS + "bbPerspectiveArea" + id, "perspare" + id, tLV, tRV, bRV, bLV);

		// Creo l'individuo perspective area e gli setto le proprietï¿½
		Node s2 = NodeFactory.createURI(NS + "PerspectiveArea" + id);
		Node p2 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o2 = NodeFactory.createURI(NS + "SpatialPerspectiveArea");
		graph.add(new Triple(s2, p2, o2));

		String str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PerspectiveArea" + id + "> <" + NS + "id> '1" + id
				+ "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s3 = NodeFactory.createURI(NS + "PerspectiveArea" + id);
		Node p3 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o3 = NodeFactory.createURI(NS + "bbPerspectiveArea" + id);
		graph.add(new Triple(s3, p3, o3));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PerspectiveArea" + id + "> <" + NS + "hfactor> '" + hF
				+ "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PerspectiveArea" + id + "> <" + NS + "wfactor> '" + wF
				+ "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PerspectiveArea" + id + "> <" + NS + "minHumanHeight> '"
				+ minH + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PerspectiveArea" + id + "> <" + NS + "minHumanWidth> '"
				+ minW + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PerspectiveArea" + id + "> <" + NS + "maxHumanHeight> '"
				+ maxH + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "PerspectiveArea" + id + "> <" + NS + "maxHumanWidth> '"
				+ maxW + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

	}

	public void createPerspectiveAreas() {
		// Prima perspective area da y=0 a y=100
		createSinglePerspectiveArea(1, new Point(0, 0), new Point(768, 100), hFactor[0], wFactor[0], minHPerspArea[0],
				minWPerspArea[0], maxHPerspArea[0], maxWPerspArea[0]);

		// Prima perspective area da y=100 a y=240
		createSinglePerspectiveArea(2, new Point(0, 101), new Point(768, 240), hFactor[1], wFactor[1], minHPerspArea[1],
				minWPerspArea[1], maxHPerspArea[1], maxWPerspArea[1]);

		// Prima perspective area da y=240 a y=380
		createSinglePerspectiveArea(3, new Point(0, 241), new Point(768, 380), hFactor[2], wFactor[2], minHPerspArea[2],
				minWPerspArea[2], maxHPerspArea[2], maxWPerspArea[2]);

		// Prima perspective area da y=380 a y=576
		createSinglePerspectiveArea(4, new Point(0, 381), new Point(768, 576), hFactor[3], wFactor[3], minHPerspArea[3],
				minWPerspArea[3], maxHPerspArea[3], maxWPerspArea[3]);
	}

	public boolean respectLowerBoundingBoxLimit(int tlx, int tly, int brx, int bry) {
		int width = brx - tlx;
		int height = bry - tly;
		boolean result = false;
		if (bry >= 0 && bry <= 100) {
			if ((width >= minWPerspArea[0] && height >= minHPerspArea[0])) {
				result = true;
			}
		} else if (bry > 100 && bry <= 240) {
			if ((width >= minWPerspArea[1] && height >= minHPerspArea[1])) {
				result = true;
			}
		} else if (bry > 240 && bry <= 380) {
			if ((width >= minWPerspArea[2] && height >= minHPerspArea[2])) {
				result = true;
			}
		} else if (bry > 380) {
			if ((width >= minWPerspArea[3] && height >= minHPerspArea[3])) {
				result = true;
			}
		}
		return result;
	}

	public boolean respectUpperBoundingBoxLimit(int tlx, int tly, int brx, int bry) {
		int width = brx - tlx;
		int height = bry - tly;
		boolean result = false;
		if (bry >= 0 && bry <= 100) {
			if ((width <= maxWPerspArea[0] && height <= maxHPerspArea[0])) {
				result = true;
			}
		} else if (bry > 100 && bry <= 240) {
			if ((width <= maxWPerspArea[1] && height <= maxHPerspArea[1])) {
				result = true;
			}
		} else if (bry > 240 && bry <= 380) {
			if ((width <= maxWPerspArea[2] && height <= maxHPerspArea[2])) {
				result = true;
			}
		} else if (bry > 380) {
			if ((width <= maxWPerspArea[3] && height <= maxHPerspArea[3])) {
				result = true;
			}
		}
		return result;
	}

	public int getPerspectiveAreaByBottomY(int bottomYCoordinate) {
		int perspective = -1;
		if (bottomYCoordinate >= 0 && bottomYCoordinate <= 100) {
			perspective = 0;
		} else if (bottomYCoordinate > 100 && bottomYCoordinate <= 240) {
			perspective = 1;
		} else if (bottomYCoordinate > 240 && bottomYCoordinate <= 380) {
			perspective = 2;
		} else if (bottomYCoordinate > 380) {
			perspective = 3;
		}
		return perspective;
	}

	public boolean sameDirection(String direction1, String direction2) {
		boolean same = false;

		if (direction1.equals("north")) {
			if (direction2.equals("nwest") || direction2.equals("nord") || (direction2.equals("nest"))) {
				same = true;
			}
		} else if (direction1.equals("nest")) {
			if (direction2.equals("north") || direction2.equals("nest") || (direction2.equals("est"))) {
				same = true;
			}
		} else if (direction1.equals("est")) {
			if (direction2.equals("nest") || direction2.equals("est") || (direction2.equals("sest"))) {
				same = true;
			}
		} else if (direction1.equals("sest")) {
			if (direction2.equals("est") || direction2.equals("sest") || (direction2.equals("south"))) {
				same = true;
			}
		} else if (direction1.equals("south")) {
			if (direction2.equals("sest") || direction2.equals("south") || (direction2.equals("swest"))) {
				same = true;
			}
		} else if (direction1.equals("swest")) {
			if (direction2.equals("south") || direction2.equals("swest") || (direction2.equals("west"))) {
				same = true;
			}
		} else if (direction1.equals("west")) {
			if (direction2.equals("swest") || direction2.equals("west") || (direction2.equals("nwest"))) {
				same = true;
			}
		} else if (direction1.equals("nwest")) {
			if (direction2.equals("west") || direction2.equals("nwest") || (direction2.equals("north"))) {
				same = true;
			}
		} else if (direction1.equals("stopped")) {
			same = true;
		}

		return same;
	}

	// txtRowInfo = {personID, topLeft.y, topLeft.x, bottomRight.y, bottomRight.x}
	public void createBlob(String individual_id, String[] txtRowInfo, String idFrame) {
		Triple t = null;
		ExtendedIterator<?> iter = null;
		
		int personId = Integer.parseInt(txtRowInfo[0]);
		int topLeftx = Integer.parseInt(txtRowInfo[2]);
		int topLeftY = Integer.parseInt(txtRowInfo[1]);
		int bottomRightX = Integer.parseInt(txtRowInfo[4]);
		int bottomRightY = Integer.parseInt(txtRowInfo[3]);

		// Creo i vertici e il centro del boundingBox del blob
		Point topLeft = new Point(topLeftx, topLeftY);
		Point bottomRight = new Point(bottomRightX, bottomRightY);
		Point topRight = new Point(bottomRightX, topLeftY);
		Point bottomLeft = new Point(topLeftx, bottomRightY);

		// Creazione dell'individuo boundingBox
		addTriple(NS + "BoundingBox" + individual_id, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", NS + "BoundingBox");

		// Settaggio delle proprietà dell'individuo boundingBox
		setBoundingBox(NS + "BoundingBox" + individual_id, individual_id, topLeft, topRight, bottomRight, bottomLeft);

		// Creazione individui della classe RGBColor
		addTriple(NS + "black", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", NS + "RGBColor");
		addTriple(NS + "red", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", NS + "RGBColor");
		addTriple(NS + "blue", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", NS + "RGBColor");

		// Definizione dell'individuo di tipo punto che rappresenta il centro del
		// bounding box
		Point center = Functions.getCenter(topLeft, bottomRight);

		addTriple(NS + "CenterPointBlob" + individual_id, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", NS + "Point");

		performInsert(NS + "CenterPointBlob" + individual_id,  NS + "x", Integer.toString(center.x));
		performInsert(NS + "CenterPointBlob" + individual_id,  NS + "y", Integer.toString(center.y));

		// Creazione dell'individuo blob
		addTriple(NS + "Blob" + individual_id, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", NS + "Blob");

		// Definizione della persona, senza però inizializzarla
		// Individual iPerson = null;

		String direction = "";
		Float speed = 0f;
		Float time = 0f;

		// Flag che dice se il bounding box ha dimensioni appropriate per la perspective
		// area a cui appartiene
		boolean lowerLimitRespect = respectLowerBoundingBoxLimit(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
		boolean upperLimitRespect = respectUpperBoundingBoxLimit(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
		boolean personIgnored = false;

		/*
		 * Se è la prima volta che appare questa persona: - avrà la proprietà
		 * "firstSeenAt" - non ha direzione (viene calcolata rispetto al frame
		 * precedente), e quindi verrà settata "hasDirection" a "stopped" - lo stesso
		 * per la proprietà "hasSpeed" messa a "0"
		 */
		Node s9 = NodeFactory.createURI(NS + "Person" + txtRowInfo[0]);
		Node p9 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o9 = NodeFactory.createURI(NS + "Person");

		if (!graph.contains(new Triple(s9, p9, o9))) {
			if (lowerLimitRespect) {
				// Creo l'individuo persona
				graph.add(new Triple(s9, p9, o9));
				
				performInsert(NS + "Person" + txtRowInfo[0], NS + "id", txtRowInfo[0]);

				addTriple(NS + "Person" + txtRowInfo[0], NS + "hasBoundingBox", NS + "BoundingBox" + individual_id);
				addTriple(NS + "Person" + txtRowInfo[0], NS + "firstSeenAt", idFrame);

				// FIX: CANCELLARE TUTTI I BLOB PRESENTI PRIMA DI AGGIUNGERE QUESTO?
				// FIX: IL REMOVE NON FUNZIONA CON ANY
				Node s121 = NodeFactory.createURI(NS + "Person" + txtRowInfo[0]);
				Node p121 = NodeFactory.createURI(NS + "blobMatch");
				iter = graph.find(s121, p121, Node.ANY);
				for (; iter.hasNext();) {
					t = (Triple) iter.next();
					graph.delete(t);
				}

				addTriple(NS + "Person" + txtRowInfo[0], NS + "blobMatch", NS + "Blob" + individual_id);

				// Imposto i valori per le proprietà  del blob
				direction = "stopped";
				speed = 0.0f;
				time = 0.0f;

				// popolo le hash map per la gestione dei cambi di direzione / frame di stessa
				// direzione
				this.lastPersonDirection.put(personId, direction);
				
				this.samePersonDirectionFrames.put(personId, 1);

				performInsert(NS + "Blob" + individual_id,  NS + "sameDirectionFrames", Integer.toString(1));

				this.directionChanges.put(personId, 0);

				performInsert( NS + "Blob" + individual_id,  NS + "directionChanges", Integer.toString(0));

				// Assegnazione proprietà colore all'individuo di classe Persona
				if (personId == 75 || personId == 102
						|| personId == 83 || personId == 108) {
					performInsert(NS + "red", NS + "hasDominatColor", "red");
					addTriple(NS + "Person" + txtRowInfo[0], NS + "hasDominatColor", NS + "red");
				} else if (personId == 25) {
					performInsert(NS + "blue", NS + "hasDominatColor", "blue");
					addTriple(NS + "Person" + txtRowInfo[0], NS + "hasDominatColor", NS + "blue");
				} else {
					performInsert(NS + "black", NS + "hasDominatColor", "black");
					addTriple(NS + "Person" + txtRowInfo[0], NS + "hasDominatColor", NS + "black");
				}
			} else {
				personIgnored = true;
			}
		} else {
			addTriple(NS + "Person" + txtRowInfo[0], NS + "hasBoundingBox", NS + "BoundingBox" + individual_id);
			addTriple(NS + "Person" + txtRowInfo[0], NS + "blobMatch", NS + "Blob" + individual_id);

			// Ottengo i punti della traiettoria relativa alla persona (in ordine discendente sull'idFrame)
			String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
					+ "SELECT ?x ?y\n"		
					+ "FROM <"+GRAPH+">\n"
					+ "WHERE {\n"
								+ "?person tracking:id \""+ txtRowInfo[0] +"\".\n"
								+ "?blob tracking:isAssociatedWith ?person;\n"
								+ "	tracking:hasBoundingBox ?bbox.\n"
								+ "?bbox tracking:hasCenter ?center.\n"
								+ "?center tracking:x ?x;\n"
								+ "	tracking:y ?y.\n"
								+ "?blob tracking:seenAtFrame ?frame.\n"
								+ "?frame tracking:id ?frameId.\n"
					+ "}\n"
					+ "ORDER BY DESC (xsd:integer(?frameId))";
			
			Query sparql = QueryFactory.create(build);
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, graph);
			ResultSet results = vqe.execSelect();
	
			// Ottengo l'ultimo punto della traiettoria (cioè il punto precedente a quello che si sta per inserire)
			QuerySolution qn = results.next();
			int x = qn.get("x").asLiteral().getInt();
			int y = qn.get("y").asLiteral().getInt();
			Point prec = new Point(x, y);
			
			// Ottengo il numero di punti presenti nella traiettoria
			int numPoints = 1;
			while(results.hasNext()) {
				numPoints++;
				results.next();
			}
			vqe.close();

			// Poi prelevo il successivo (ossia quella attuale) 
			Point post = new Point(center.x, center.y);

			// Imposto i valori per le proprietà del blob
			direction = Functions.getDirection(prec, post);
			speed = Functions.Speed(prec, post);
			time = (float) numPoints * 0.14f;

			// Popolo le hash map per la gestione dei cambi di direzione / frame di stessa
			// direzione
			int num = samePersonDirectionFrames.get(personId) + 1;
			samePersonDirectionFrames.put(personId, num);
			performInsert(NS + "Blob" + individual_id, NS + "sameDirectionFrames", Integer.toString(num));

			performInsert(NS + "Blob" + individual_id, NS + "directionChanges", Integer.toString(directionChanges.get(personId)));

			if (!lastPersonDirection.get(personId).equals(direction)) {
				Integer newPersDir = newPersonDirection.get(personId);
				if (newPersDir == null) {
					newPersonDirection.put(personId, 1);
				} else {
					newPersonDirection.put(personId, newPersDir + 1);
				}
				if (newPersonDirection.get(personId) > 7) {
					lastPersonDirection.put(personId, direction);
					samePersonDirectionFrames.put(personId, 7);
					
					int dirChanges = directionChanges.get(personId) + 1;
					directionChanges.put(personId, dirChanges);

					performInsert(NS + "Blob" + individual_id, NS + "sameDirectionFrames", Integer.toString(7));
					performInsert(NS + "Blob" + individual_id, NS + "directionChanges", Integer.toString(dirChanges));

					newPersonDirection.remove(personId);
				}
			}
		}

		// Setto le restanti proprietà a blob e boundingBox
		performInsert(NS + "Blob" + individual_id, NS + "blobid", individual_id);

		addTriple(NS + "Blob" + individual_id, NS + "hasBoundingBox", NS + "BoundingBox" + individual_id);
		addTriple(NS + "Blob" + individual_id, NS + "seenAtFrame", idFrame);
		addTriple(NS + "BoundingBox" + individual_id, NS + "hasCenter", NS + "CenterPointBlob" + individual_id);
	
		// Setto la proprietà di blob malformato
		if (!lowerLimitRespect) {
			performInsert(NS + "Blob" + individual_id, NS + "malformedBlob", "true");
		}

		// Se la persona non è stata ignorata (e quindi sono stati calcolati
		// gli opportuni valori) setto queste proprietÃ  al blob
		if (!personIgnored) {
			// Setto l'associazione tra blob e persona
			addTriple(NS + "Blob" + individual_id, NS + "isAssociatedWith", NS + "Person" + txtRowInfo[0]);
			
			// Setto la direzione 
			addTriple(NS + direction, "http://www.w3.org/2000/01/rdf-schema#domain", NS + "Blob");

			performInsert(NS + direction, "http://www.w3.org/2000/01/rdf-schema#label", direction);

			addTriple(NS + "Blob" + individual_id, NS + "hasDirection", NS + direction);
			
			// Setto la velocità
			performInsert(NS + "Blob" + individual_id, NS + "hasSpeed", String.valueOf(speed));

			// Setto il tempo 
			performInsert(NS + "Blob" + individual_id, NS + "timeOnScene", String.valueOf(time));
		}

		// Setto la proprietà di probabile gruppo
		if (!upperLimitRespect) {
			if (trackingOutputFile.equals("src//view1.txt")) {
				performInsert(NS + "Person" + personId, NS + "isProbablyAGroup", "true");
			}
		}

		/*
		 * Ora si procede al calcolo dell'area in cui si trova il BoundingBox relativo
		 * alla persona i-esima (Entry,Exit,Occluding)
		 */
		/*
		 * indice 0 = entryArea1: in basso a destra; indice 1 = entryArea2: in alto un
		 * po' a sinistra indice 2 = entryArea3: in alto un po' a destra indice 3 =
		 * exitArea1: sopra a sinistra indice 4 = exitArea2: un po' più in basso a
		 * sinistra indice 5 = occludingArea1: il palo compreso il cartello attaccato
		 * indice 6 = occludingArea2: oggetto sulla strada in alto un po' a sinistra
		 * indice 7 = passingArea1
		 */
		if (rect.get(0).contains(bottomLeft)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "EntryArea1");
		} else if (rect.get(1).contains(bottomLeft)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "EntryArea2");
		}
		if (rect.get(2).contains(bottomLeft)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "EntryArea3");
		} else if (rect.get(3).contains(bottomLeft)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "ExitArea1");
		} else if (rect.get(4).contains(bottomLeft)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "ExitArea2");
		} else if (rect.get(5).contains(bottomLeft) || rect.get(5).contains(bottomRight)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "OccludingArea1");
		} else if (rect.get(6).contains(bottomLeft) || rect.get(6).contains(bottomRight)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "OccludingArea2");
		} else if (rect.get(7).contains(bottomRight) && rect.get(7).contains(bottomLeft)) {
			addTriple(NS + "Blob" + individual_id, NS + "isLocatedin", NS + "PassingArea1");
		}
	}

	public void createGroups(ArrayList<String> peopleOfAFrame, String frame) {
		Node blob = null, boundingBox = null;
		Triple t = null, currentMember_triple;
		String build, direction1 = null, direction2 = null, bRV_x_st, bLV_x_st, bLV_y_st, frameId_st, currentMember,
				last_seen_frame_sub = null, first_seen_frame_sub = null, dir2 = null, bRV = null, bLV = null,
				dir1 = null, last_seen_frame_st = null, first_seen_frame_st = null;
		int x1, y1, x2 = 0, y2 = 0;
		String currentGroup, str;
		boolean sameDirection, existingGroup, containFirstPerson, containSecondPerson, i1_isProbablyAGroup = false,
				i2_isProbablyAGroup = false;
		int perspective1, perspective2, last_seen_frame = 0, first_seen_frame = 0;
		Iterator<String> groupIt;
		ExtendedIterator<?> memberIt, iter;
		int groupOldness;
		int groupSince;
		VirtuosoUpdateRequest vur;
		VirtuosoQueryExecution vqe;
		RDFNode bRV_x = null, bLV_x = null, bLV_y = null, direct1 = null, direct2 = null, Node_frameId = null,
				Node_last_seen = null, Node_first_seen = null;
		ResultSet results;

		for (String individ1 : peopleOfAFrame) { // Ciclo sulle persone
			for (String individ2 : peopleOfAFrame) { // Ciclo sulle persone
				// Per le coppie che non includono la stessa persona
				if (individ1 != null && individ2 != null && !individ1.equals(individ2)) {

					// Ottengo il blob dalla persona
					Node s1 = NodeFactory.createURI(NS + individ1);
					Node p1 = NodeFactory.createURI(NS + "blobMatch");
					iter = graph.find(s1, p1, Node.ANY);

					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						blob = t.getObject();
					}
					// Ottengo la direzione della persona
					Node p2 = NodeFactory.createURI(NS + "hasDirection");
					iter = graph.find(blob, p2, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						dir1 = t.getObject().toString();
					}

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + dir1
							+ "> <http://www.w3.org/2000/01/rdf-schema#label> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						direct1 = rs.get("o");
					}
					direction1 = direct1.asLiteral().toString();

					// Ottengo il bounding box dal blob
					Node p3 = NodeFactory.createURI(NS + "hasBoundingBox");
					iter = graph.find(blob, p3, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						boundingBox = t.getObject();
					}

					// Ottengo le coordinate del centro dei piedi della persona
					Node p4 = NodeFactory.createURI(NS + "bottomLeftVertex");
					iter = graph.find(boundingBox, p4, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						bLV = t.getObject().toString();
					}

					Node p5 = NodeFactory.createURI(NS + "bottomRightVertex");
					iter = graph.find(boundingBox, p5, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						bRV = t.getObject().toString();
					}

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + bLV + "> <" + NS + "x> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						bLV_x = rs.get("o");
					}
					bLV_x_st = bLV_x.asLiteral().toString();

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + bRV + "> <" + NS + "x> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						bRV_x = rs.get("o");
					}
					bRV_x_st = bRV_x.asLiteral().toString();

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + bLV + "> <" + NS + "y> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						bLV_y = rs.get("o");
					}
					bLV_y_st = bLV_y.asLiteral().toString();

					x1 = Math.round((Integer.parseInt(bLV_x_st) + Integer.parseInt(bRV_x_st)) / 2);
					y1 = Integer.parseInt(bLV_y_st);
					// System.out.println(blob.getLocalName());
					blob = null;
					boundingBox = null;
					bLV = null;
					bLV = null;

					// Ottengo il blob dalla persona
					Node s2 = NodeFactory.createURI(NS + individ2);
					Node p9 = NodeFactory.createURI(NS + "blobMatch");
					iter = graph.find(s2, p9, Node.ANY);

					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						blob = t.getObject();
					}

					// Ottendgo la direzione della persona
					Node p10 = NodeFactory.createURI(NS + "hasDirection");
					iter = graph.find(blob, p10, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						dir2 = t.getObject().toString();
					}

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + dir2
							+ "> <http://www.w3.org/2000/01/rdf-schema#label> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						direct2 = rs.get("o");
					}
					direction2 = direct2.asLiteral().toString();

					// Ottengo il bounding box dal blob
					Node p11 = NodeFactory.createURI(NS + "hasBoundingBox");
					iter = graph.find(blob, p11, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						boundingBox = t.getObject();
					}

					// Ottengo le coordinate del centro dei piedi della persona
					Node p12 = NodeFactory.createURI(NS + "bottomLeftVertex");
					iter = graph.find(boundingBox, p12, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						bLV = t.getObject().toString();
					}

					Node p13 = NodeFactory.createURI(NS + "bottomRightVertex");
					iter = graph.find(boundingBox, p13, Node.ANY);
					for (; iter.hasNext();) {
						t = (Triple) iter.next();
						bRV = t.getObject().toString();
					}

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + bLV + "> <" + NS + "x> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						bLV_x = rs.get("o");
					}
					bLV_x_st = bLV_x.asLiteral().toString();

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + bRV + "> <" + NS + "x> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						bRV_x = rs.get("o");
					}
					bRV_x_st = bRV_x.asLiteral().toString();

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + bLV + "> <" + NS + "y> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						bLV_y = rs.get("o");
					}
					bLV_y_st = bLV_y.asLiteral().toString();

					x2 = Math.round((Integer.parseInt(bLV_x_st) + Integer.parseInt(bRV_x_st)) / 2);
					y2 = Integer.parseInt(bLV_y_st);

					// Calcolo la perspective di riferimento delle due persone
					perspective1 = getPerspectiveAreaByBottomY(y1);
					perspective2 = getPerspectiveAreaByBottomY(y2);

					// Calcolo le opportune soglie come media delle soglie delle rispettive
					// perspective
					// (NB: se le perspective sono le stesse si ottengono le soglie proprie di
					// quella perspective)
					int threshX = Math.round(
							(xGropuThreshold * wFactor[perspective1] + xGropuThreshold * wFactor[perspective2]) / 2);
					int threshY = Math.round(
							(yGropuThreshold * hFactor[perspective1] + yGropuThreshold * hFactor[perspective2]) / 2);

					build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + frame + "> <" + NS + "id> ?o }";
					vqe = VirtuosoQueryExecutionFactory.create(build, graph);

					results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution rs = results.nextSolution();
						Node_frameId = rs.get("o");
					}
					frameId_st = Node_frameId.asLiteral().toString();
					int frameId = Integer.parseInt(frameId_st);

					if (frameId > 64 && frameId < 89 && ((individ1.equals("Person1") && individ2.equals("Person4"))
							|| (individ1.equals("Person4") && individ2.equals("Person1")))) {
						// System.out.println(individ1.getLocalName()+" "+individ2.getLocalName());
						// System.out.println("perspective "+perspective1+" "+perspective2);
						// System.out.println("soglie "+threshX+" "+threshY);
						// System.out.println("x1 e y1 "+x1+" "+y1);
						// System.out.println("x2 e y2 "+x2+" "+y2);
						// System.out.println("distanze in x e y "+Math.abs(x1-x2)+" "+Math.abs(y1-y2));
						// System.out.println("direzioni "+direction1+" "+direction2);
					}

					// Controllo se i due blob hanno la stessa direzione
					sameDirection = sameDirection(direction1, direction2);

					if (sameDirection && (Math.abs(x1 - x2) <= threshX) && (Math.abs(y1 - y2) <= threshY)) { // Da
																												// considerare
																												// gruppo
						existingGroup = false;
						groupIt = groups.iterator();

						// Scorro tutti i gruppi esistenti
						while (groupIt.hasNext() && !existingGroup) {
							currentGroup = groupIt.next();
							containFirstPerson = false;
							containSecondPerson = false;

							Node s18 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + currentGroup);
							Node p18 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/member");
							memberIt = graph.find(s18, p18, Node.ANY);

							// Controllo se il generico gruppo esistente contiene già le due persone in
							// esame
							while (memberIt.hasNext()) {
								currentMember_triple = (Triple) memberIt.next();
								currentMember = currentMember_triple.getObject().toString();
								currentMember = currentMember.substring(currentMember.indexOf("#") + 1);

								if (currentMember.equals(individ1)) {
									containFirstPerson = true;
								} else if (currentMember.equals(individ2)) {
									containSecondPerson = true;
								}
							}
							// Se il gruppo non è più vecchio di un certo numero di frame
							Node s19 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + currentGroup);
							Node p19 = NodeFactory.createURI(NS + "lastSeenAt");
							iter = graph.find(s19, p19, Node.ANY);
							for (; iter.hasNext();) {
								t = (Triple) iter.next();
								last_seen_frame_sub = t.getObject().toString();
							}

							build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + last_seen_frame_sub + "> <" + NS
									+ "id> ?o }";
							vqe = VirtuosoQueryExecutionFactory.create(build, graph);

							results = vqe.execSelect();
							while (results.hasNext()) {
								QuerySolution rs = results.nextSolution();
								Node_last_seen = rs.get("o");
							}
							if (Node_last_seen != null) {
								last_seen_frame_st = Node_last_seen.asLiteral().toString();
								last_seen_frame = Integer.parseInt(last_seen_frame_st);
							}
							groupOldness = frameId - last_seen_frame;

							// Se le contiene allora aggiorno solo la proprietà "lastSeenAt"
							if (groupOldness <= oldGroupThreshold && containFirstPerson && containSecondPerson) {
								Node s201 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + currentGroup);
								Node p201 = NodeFactory.createURI(NS + "lastSeenAt");
								iter = graph.find(s201, p201, Node.ANY);
								for (; iter.hasNext();) {
									t = (Triple) iter.next();
									graph.delete(t);
								}

								Node s21 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + currentGroup);
								Node p21 = NodeFactory.createURI(NS + "lastSeenAt");
								Node o21 = NodeFactory.createURI(frame);
								graph.add(new Triple(s21, p21, o21));

								Node s22 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + currentGroup);
								Node p22 = NodeFactory.createURI(NS + "firstSeenAt");
								iter = graph.find(s22, p22, Node.ANY);
								for (; iter.hasNext();) {
									t = (Triple) iter.next();
									first_seen_frame_sub = t.getObject().toString();
								}

								build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + first_seen_frame_sub + "> <" + NS
										+ "id> ?o }";
								vqe = VirtuosoQueryExecutionFactory.create(build, graph);

								results = vqe.execSelect();
								while (results.hasNext()) {
									QuerySolution rs = results.nextSolution();
									Node_first_seen = rs.get("o");
								}
								if (Node_first_seen != null) {
									first_seen_frame_st = Node_first_seen.asLiteral().toString();
									first_seen_frame = Integer.parseInt(first_seen_frame_st);
								}
								groupSince = last_seen_frame - first_seen_frame + 1;

								build = "SELECT * FROM <" + GRAPH + "> WHERE { <http://xmlns.com/foaf/0.1/"
										+ currentGroup + "> <" + NS + "groupSince> ?o }";
								vqe = VirtuosoQueryExecutionFactory.create(build, graph);
								results = vqe.execSelect();
								while (results.hasNext()) {
									QuerySolution res = results.nextSolution();
									String obj = res.get("o").asLiteral().toString();
									str = "DELETE FROM GRAPH <" + GRAPH + "> { <http://xmlns.com/foaf/0.1/"
											+ currentGroup + "> <" + NS + "groupSince> '" + obj + "' }";
									vur = VirtuosoUpdateFactory.create(str, graph);
									vur.exec();
								}

								str = "INSERT INTO GRAPH <" + GRAPH + "> { <http://xmlns.com/foaf/0.1/" + currentGroup
										+ "> <" + NS + "groupSince> '" + groupSince + "'}";
								vur = VirtuosoUpdateFactory.create(str, graph);
								vur.exec();

								existingGroup = true;
								// System.out.println("Modificato il gruppo "+currentGroup.getLocalName()+" con
								// le persone "+individ1.getLocalName()+" "+individ2.getLocalName());
							}
						}

						// Se il gruppo formato dalla coppia di persone in esame non esiste allora lo
						// creo
						if (!existingGroup) {
							Node s26 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + "Group" + groupId);
							Node p26 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
							Node o26 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/Group");
							graph.add(new Triple(s26, p26, o26));

							// System.out.println("Creato il gruppo "+newGroup.getLocalName()+" con le
							// persone "+individ1.getLocalName()+" "+individ2.getLocalName());
							Node s27 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + "Group" + groupId);
							Node p27 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/member");
							Node o27 = NodeFactory.createURI(NS + individ1);
							graph.add(new Triple(s27, p27, o27));

							Node s28 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + "Group" + groupId);
							Node p28 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/member");
							Node o28 = NodeFactory.createURI(NS + individ2);
							graph.add(new Triple(s28, p28, o28));

							str = "INSERT INTO GRAPH <" + GRAPH + "> { <http://xmlns.com/foaf/0.1/Group" + groupId
									+ "> <" + NS + "id> '" + groupId + "'}";
							vur = VirtuosoUpdateFactory.create(str, graph);
							vur.exec();

							Node s30 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + "Group" + groupId);
							Node p30 = NodeFactory.createURI(NS + "lastSeenAt");
							Node o30 = NodeFactory.createURI(frame);
							graph.add(new Triple(s30, p30, o30));

							Node s31 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/" + "Group" + groupId);
							Node p31 = NodeFactory.createURI(NS + "firstSeenAt");
							Node o31 = NodeFactory.createURI(frame);
							graph.add(new Triple(s31, p31, o31));

							groupSince = 1;

							build = "SELECT * FROM <" + GRAPH + "> WHERE { <http://xmlns.com/foaf/0.1/Group" + groupId
									+ "> <" + NS + "groupSince> ?o }";
							vqe = VirtuosoQueryExecutionFactory.create(build, graph);
							results = vqe.execSelect();
							while (results.hasNext()) {
								QuerySolution res = results.nextSolution();
								String obj = res.get("o").asLiteral().toString();
								str = "DELETE FROM GRAPH <" + GRAPH + "> { <http://xmlns.com/foaf/0.1/Group" + groupId
										+ "> <" + NS + "groupSince> '" + obj + "' }";
								vur = VirtuosoUpdateFactory.create(str, graph);
								vur.exec();
							}

							str = "INSERT INTO GRAPH <" + GRAPH + "> { <http://xmlns.com/foaf/0.1/Group" + groupId
									+ "> <" + NS + "groupSince> '" + groupSince + "'}";
							vur = VirtuosoUpdateFactory.create(str, graph);
							vur.exec();

							groups.add("Group" + groupId);

							// Se una delle due persone era probabilmente un gruppo allora viene settato che
							// il gruppo
							// ï¿½ tale dall'entrata in scena di entrambe le persone, ovvero non ï¿½ nato da
							// un incontro
							boolean flag1 = false;
							build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + NS + individ1 + "> <" + NS
									+ "isProbablyAGroup> ?o }";
							vqe = VirtuosoQueryExecutionFactory.create(build, graph);
							results = vqe.execSelect();
							while (results.hasNext()) {
								QuerySolution rs = results.nextSolution();
								flag1 = true;
								i1_isProbablyAGroup = Boolean.parseBoolean(rs.get("o").asLiteral().toString());
							}

							boolean flag2 = false;
							build = "SELECT * FROM <" + GRAPH + "> WHERE { <" + NS + individ2 + "> <" + NS
									+ "isProbablyAGroup> ?o }";
							vqe = VirtuosoQueryExecutionFactory.create(build, graph);
							results = vqe.execSelect();
							while (results.hasNext()) {
								QuerySolution rs = results.nextSolution();
								flag2 = true;
								i2_isProbablyAGroup = Boolean.parseBoolean(rs.get("o").asLiteral().toString());
							}

							if ((flag1 && i1_isProbablyAGroup) || (flag2 && i2_isProbablyAGroup)) {
								str = "INSERT INTO GRAPH <" + GRAPH + "> { <http://xmlns.com/foaf/0.1/Group" + groupId
										+ "> <" + NS + "groupSinceEntry> 'true'}";
								vur = VirtuosoUpdateFactory.create(str, graph);
								vur.exec();
							} else {
								str = "INSERT INTO GRAPH <" + GRAPH + "> { <http://xmlns.com/foaf/0.1/Group" + groupId
										+ "> <" + NS + "groupSinceEntry> 'false'}";
								vur = VirtuosoUpdateFactory.create(str, graph);
								vur.exec();
							}
							groupId++;
						}
					}
				}
			}
		}
	}

	/* Attraverso tale metodo creo tutti gli oggetti statici della scena: 
	 * l'id lo scegliamo noi tanto tali oggetti sono statici.
	 * Anche in questo caso, definisco prima i vertici dei BoundingBoxes, poi i
	 * boundingBoxes e poi le Aree e gli occludingObjects.
	 */

	/*
	 * NOTA IMPORTANTE: tale metodo restituisce un Array di rettangoli che rappresentano
	 * le aree relative alle Entry, Exit e Occluding Area; ciò viene fatto dato che
	 * in questo modo abbiamo possibilità di conoscere facilmente se un punto (il
	 * centro del BoundingBox relativo alla persona i-esima) è presente
	 * all'interno di tali aree.
	 */
	public ArrayList<Rectangle> createStaticThings() {
		VirtuosoUpdateRequest vur;
		String str;
		rect = new ArrayList<Rectangle>();

		Point topLeft;
		Point bottomRight;
		Point topRight;
		Point bottomLeft;

		// ObjectProperty hasBoundingBox = otp.hasBoundingBox;
		// DatatypeProperty idArea = dtp.id;

		/* Prima EntryArea (in basso a destra) */

		topLeft = new Point(724, 270);
		bottomRight = new Point(767, 576);
		topRight = new Point(767, 270);
		bottomLeft = new Point(724, 576);

		/* Creo prima il BoundingBox */
		Node s1 = NodeFactory.createURI(NS + "EntryArea1");
		Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o1 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s1, p1, o1));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "EntryArea1", "ena1", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s2 = NodeFactory.createURI(NS + "EntryArea1");
		Node p2 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o2 = NodeFactory.createURI(NS + "EntryArea");
		graph.add(new Triple(s2, p2, o2));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+EntryArea1> <" + NS + "id> '01'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s4 = NodeFactory.createURI(NS + "EntryArea1");
		Node p4 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o4 = NodeFactory.createURI(NS + "EntryArea1");
		graph.add(new Triple(s4, p4, o4));

		/*
		 * Definisco un rettangolo che tornerà utile per il controllo
		 * dell'intersezione di un BoundingBox con una specifia area
		 */
		int width = topRight.x - topLeft.x;
		int height = bottomLeft.y - topLeft.y;
		Rectangle rect_entry1 = new Rectangle(724, 270, width, height);
		rect.add(rect_entry1);

		/* Seconda EntryArea (in alto un po' a sinistra) */

		topLeft = new Point(280, 58);
		bottomRight = new Point(309, 92);
		topRight = new Point(309, 58);
		bottomLeft = new Point(280, 92);

		/* Creo prima il BoundingBox */
		Node s5 = NodeFactory.createURI(NS + "EntryArea2");
		Node p5 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o5 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s5, p5, o5));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "EntryArea2", "ena2", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s6 = NodeFactory.createURI(NS + "EntryArea2");
		Node p6 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o6 = NodeFactory.createURI(NS + "EntryArea");
		graph.add(new Triple(s6, p6, o6));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+EntryArea2> <" + NS + "id> '02'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s8 = NodeFactory.createURI(NS + "EntryArea2");
		Node p8 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o8 = NodeFactory.createURI(NS + "EntryArea2");
		graph.add(new Triple(s8, p8, o8));

		width = topRight.x - topLeft.x;
		height = bottomLeft.y - topLeft.y;
		Rectangle rect_entry2 = new Rectangle(280, 58, width, height);
		rect.add(rect_entry2);

		/* Terza EntryArea (in alto un po' a destra) */

		topLeft = new Point(748, 28);
		bottomRight = new Point(768, 61);
		topRight = new Point(768, 28);
		bottomLeft = new Point(748, 61);

		/* Creo prima il BoundingBox */
		Node s9 = NodeFactory.createURI(NS + "EntryArea3");
		Node p9 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o9 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s9, p9, o9));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "EntryArea3", "ena3", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s10 = NodeFactory.createURI(NS + "EntryArea3");
		Node p10 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o10 = NodeFactory.createURI(NS + "EntryArea");
		graph.add(new Triple(s10, p10, o10));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+EntryArea3> <" + NS + "id> '03'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s12 = NodeFactory.createURI(NS + "EntryArea3");
		Node p12 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o12 = NodeFactory.createURI(NS + "EntryArea3");
		graph.add(new Triple(s12, p12, o12));

		width = topRight.x - topLeft.x;
		height = bottomLeft.y - topLeft.y;
		Rectangle rect_entry3 = new Rectangle(748, 28, width, height);
		rect.add(rect_entry3);

		/* Prima ExitArea (sopra a sinistra) */

		topLeft = new Point(0, 128);
		bottomRight = new Point(17, 152);
		topRight = new Point(17, 128);
		bottomLeft = new Point(0, 152);

		/* Creo prima il BoundingBox */
		Node s13 = NodeFactory.createURI(NS + "ExitArea1");
		Node p13 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o13 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s13, p13, o13));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "ExitArea1", "exa1", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s14 = NodeFactory.createURI(NS + "ExitArea1");
		Node p14 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o14 = NodeFactory.createURI(NS + "ExitArea");
		graph.add(new Triple(s14, p14, o14));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+ExitArea1> <" + NS + "id> '04'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s16 = NodeFactory.createURI(NS + "ExitArea1");
		Node p16 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o16 = NodeFactory.createURI(NS + "ExitArea1");
		graph.add(new Triple(s16, p16, o16));

		width = topRight.x - topLeft.x;
		height = bottomLeft.y - topLeft.y;
		Rectangle rect_exit1 = new Rectangle(0, 128, width, height);
		rect.add(rect_exit1);

		/* Seconda ExitArea (un po' piï¿½ in basso a sinistra) */

		topLeft = new Point(0, 165);
		bottomRight = new Point(34, 245);
		topRight = new Point(34, 165);
		bottomLeft = new Point(0, 245);

		/* Creo prima il BoundingBox */
		Node s17 = NodeFactory.createURI(NS + "ExitArea2");
		Node p17 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o17 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s17, p17, o17));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "ExitArea2", "exa2", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s18 = NodeFactory.createURI(NS + "ExitArea2");
		Node p18 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o18 = NodeFactory.createURI(NS + "ExitArea");
		graph.add(new Triple(s18, p18, o18));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+ExitArea2> <" + NS + "id> '05'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s20 = NodeFactory.createURI(NS + "ExitArea2");
		Node p20 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o20 = NodeFactory.createURI(NS + "ExitArea2");
		graph.add(new Triple(s20, p20, o20));

		width = topRight.x - topLeft.x;
		height = bottomLeft.y - topLeft.y;
		Rectangle rect_exit2 = new Rectangle(0, 165, width, height);
		rect.add(rect_exit2);

		/* Prima OccludingArea (il palo compreso il cartello attaccato) */

		topLeft = new Point(413, 87);
		bottomRight = new Point(444, 391);
		topRight = new Point(444, 87);
		bottomLeft = new Point(413, 391);

		/* Creo prima il BoundingBox */
		Node s21 = NodeFactory.createURI(NS + "OccludingArea1");
		Node p21 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o21 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s21, p21, o21));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "OccludingArea1", "oca1", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s22 = NodeFactory.createURI(NS + "OccludingArea1");
		Node p22 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o22 = NodeFactory.createURI(NS + "OArea");
		graph.add(new Triple(s22, p22, o22));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+OccludingArea1> <" + NS + "id> '06'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s24 = NodeFactory.createURI(NS + "OccludingArea1");
		Node p24 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o24 = NodeFactory.createURI(NS + "OccludingArea1");
		graph.add(new Triple(s24, p24, o24));

		width = topRight.x - topLeft.x;
		height = bottomLeft.y - topLeft.y;
		Rectangle rect_occl1 = new Rectangle(413, 87, width, height);
		rect.add(rect_occl1);

		/* Seconda OccludingArea (oggetto sulla strada in alto un po' a sinistra) */

		topLeft = new Point(30, 112);
		bottomRight = new Point(49, 141);
		topRight = new Point(49, 112);
		bottomLeft = new Point(30, 141);

		/* Creo prima il BoundingBox */
		Node s25 = NodeFactory.createURI(NS + "OccludingArea2");
		Node p25 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o25 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s25, p25, o25));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "OccludingArea2", "oca2", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s26 = NodeFactory.createURI(NS + "OccludingArea2");
		Node p26 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o26 = NodeFactory.createURI(NS + "OArea");
		graph.add(new Triple(s26, p26, o26));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+OccludingArea2> <" + NS + "id> '07'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s28 = NodeFactory.createURI(NS + "OccludingArea2");
		Node p28 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o28 = NodeFactory.createURI(NS + "OccludingArea2");
		graph.add(new Triple(s28, p28, o28));

		width = topRight.x - topLeft.x;
		height = bottomLeft.y - topLeft.y;
		Rectangle rect_occl2 = new Rectangle(30, 112, width, height);
		rect.add(rect_occl2);

		/* Primo Occluding Object (il palo) */

		topLeft = new Point(422, 103);
		bottomRight = new Point(436, 391);
		topRight = new Point(436, 103);
		bottomLeft = new Point(422, 391);

		/* Creo prima il BoundingBox */
		Node s29 = NodeFactory.createURI(NS + "OccludingObject1");
		Node p29 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o29 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s29, p29, o29));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "OccludingObject1", "oco1", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s30 = NodeFactory.createURI(NS + "OccludingObject1");
		Node p30 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o30 = NodeFactory.createURI(NS + "ThinVertical");
		graph.add(new Triple(s30, p30, o30));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+OccludingObject1> <" + NS + "id> '001'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s32 = NodeFactory.createURI(NS + "OccludingObject1");
		Node p32 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o32 = NodeFactory.createURI(NS + "OccludingObject1");
		graph.add(new Triple(s32, p32, o32));

		/* Secondo Occluding Object (il cartello attaccato al palo) */

		topLeft = new Point(411, 200);
		bottomRight = new Point(446, 245);
		topRight = new Point(446, 200);
		bottomLeft = new Point(411, 245);

		/* Creo prima il BoundingBox */
		Node s33 = NodeFactory.createURI(NS + "OccludingObject2");
		Node p33 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o33 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s33, p33, o33));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "OccludingObject2", "oco2", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s34 = NodeFactory.createURI(NS + "OccludingObject2");
		Node p34 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o34 = NodeFactory.createURI(NS + "Squared");
		graph.add(new Triple(s34, p34, o34));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+OccludingObject2> <" + NS + "id> '002'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s36 = NodeFactory.createURI(NS + "OccludingObject2");
		Node p36 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o36 = NodeFactory.createURI(NS + "OccludingObject2");
		graph.add(new Triple(s36, p36, o36));

		/*
		 * Terzo Occluding Object (l'oggetto sulla strada in alto un po' a sinistra
		 * [equivale alla OArea di prima])
		 */

		topLeft = new Point(30, 112);
		bottomRight = new Point(49, 141);
		topRight = new Point(49, 112);
		bottomLeft = new Point(30, 141);

		/* Creo prima il BoundingBox */
		Node s37 = NodeFactory.createURI(NS + "OccludingObject3");
		Node p37 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o37 = NodeFactory.createURI(NS + "BoundingBox");
		graph.add(new Triple(s37, p37, o37));

		/* Con tale metodo setto il BoundingBox */
		setBoundingBox(NS + "OccludingObject3", "oco3", topLeft, topRight, bottomRight, bottomLeft);

		/* Creo l'istanza relativa alla specifica area */
		Node s38 = NodeFactory.createURI(NS + "OccludingObject3");
		Node p38 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o38 = NodeFactory.createURI(NS + "Squared");
		graph.add(new Triple(s38, p38, o38));

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "+OccludingObject3> <" + NS + "id> '003'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		Node s40 = NodeFactory.createURI(NS + "OccludingObject3");
		Node p40 = NodeFactory.createURI(NS + "hasBoundingBox");
		Node o40 = NodeFactory.createURI(NS + "OccludingObject3");
		graph.add(new Triple(s40, p40, o40));

		return rect;
	}

	/* In questo metodo setto l'i-esimo BoundingBox */
	public void setBoundingBox(String ind, String individual_id, Point topLeft, Point topRight, Point bottomRight,
			Point bottomLeft) {
		VirtuosoUpdateRequest vur;
		String str;

		// Aggiunta dei Point
		Node s1 = NodeFactory.createURI(NS + "TopLeftPoint" + individual_id);
		Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o1 = NodeFactory.createURI(NS + "Point");
		graph.add(new Triple(s1, p1, o1));

		Node s2 = NodeFactory.createURI(NS + "TopRightPoint" + individual_id);
		Node p2 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o2 = NodeFactory.createURI(NS + "Point");
		graph.add(new Triple(s2, p2, o2));

		Node s3 = NodeFactory.createURI(NS + "BottomLeftPoint" + individual_id);
		Node p3 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o3 = NodeFactory.createURI(NS + "Point");
		graph.add(new Triple(s3, p3, o3));

		Node s4 = NodeFactory.createURI(NS + "BottomRightPoint" + individual_id);
		Node p4 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o4 = NodeFactory.createURI(NS + "Point");
		graph.add(new Triple(s4, p4, o4));

		// Aggiunta delle properties x e y per ogni punto
		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "TopLeftPoint" + individual_id + "> <" + NS + "x> '"
				+ topLeft.x + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "TopLeftPoint" + individual_id + "> <" + NS + "y> '"
				+ topLeft.y + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "TopRightPoint" + individual_id + "> <" + NS + "x> '"
				+ topRight.x + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "TopRightPoint" + individual_id + "> <" + NS + "y> '"
				+ topRight.y + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "BottomLeftPoint" + individual_id + "> <" + NS + "x> '"
				+ bottomLeft.x + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "BottomLeftPoint" + individual_id + "> <" + NS + "y> '"
				+ bottomLeft.y + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "BottomRightPoint" + individual_id + "> <" + NS + "x> '"
				+ bottomRight.x + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		str = "INSERT INTO GRAPH <" + GRAPH + "> { <" + NS + "BottomRightPoint" + individual_id + "> <" + NS + "y> '"
				+ bottomRight.y + "'}";
		vur = VirtuosoUpdateFactory.create(str, graph);
		vur.exec();

		// Aggiunta del valore delle properties
		Node s13 = NodeFactory.createURI(ind);
		Node p13 = NodeFactory.createURI(NS + "bottomRightVertex");
		Node o13 = NodeFactory.createURI(NS + "BottomRightPoint" + individual_id);
		graph.add(new Triple(s13, p13, o13));

		Node s14 = NodeFactory.createURI(ind);
		Node p14 = NodeFactory.createURI(NS + "bottomLeftVertex");
		Node o14 = NodeFactory.createURI(NS + "BottomLeftPoint" + individual_id);
		graph.add(new Triple(s14, p14, o14));

		Node s15 = NodeFactory.createURI(ind);
		Node p15 = NodeFactory.createURI(NS + "topRightVertex");
		Node o15 = NodeFactory.createURI(NS + "TopRightPoint" + individual_id);
		graph.add(new Triple(s15, p15, o15));

		Node s16 = NodeFactory.createURI(ind);
		Node p16 = NodeFactory.createURI(NS + "topLeftVertex");
		Node o16 = NodeFactory.createURI(NS + "TopLeftPoint" + individual_id);
		graph.add(new Triple(s16, p16, o16));
	}

	public void saveParameters(int idBlob) {
		ArrayList<Object> param = new ArrayList<Object>();
		param.add(idBlob);
		param.add(this.groupId);
		param.add(this.groups);
		param.add(this.lastPersonDirection);
		param.add(this.samePersonDirectionFrames);
		param.add(this.directionChanges);

		try {
			FileOutputStream fileOut = new FileOutputStream("src/saved_parameters.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(param);
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in /src/saved_parameters.ser");
		} catch (IOException i) {
			i.printStackTrace();
		}
	}
	
	private void addTriple(String subjUri, String predUri, String objUri) {
		Node s = NodeFactory.createURI(subjUri);
		Node p = NodeFactory.createURI(predUri);
		Node o = NodeFactory.createURI(objUri);
		graph.add(new Triple(s, p, o));
	}
	
	private void performInsert(String subj, String pred, String obj) {
		String s = "INSERT INTO GRAPH <" + GRAPH + "> { <" + subj + "> <" + pred + "> '"
				+ obj + "'}";
		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(s, graph);
		vur.exec();
	}
}
