package tracking;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.resultset.ResultSetMem;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

@SuppressWarnings("unused")
public class SparqlQueries {
	private static final String GRAPH = "http://localhost:8890/TSSEProject";
	private static final String NS = "http://mivia.unisa.it/videotracking/tracking.owl#";
	public static final int QUERY_TYPE_ON_PERSON = 0;
	public static final int QUERY_TYPE_ON_GROUP = 1;
	public static final int QUERY_1 = 1;
	public static final int QUERY_2 = 2;
	public static final int QUERY_3 = 3;
	public static final int QUERY_4 = 4;
	public static final int QUERY_5 = 5;
	public static final int QUERY_6 = 6;
	public static final int QUERY_7 = 7;
	public static final int QUERY_8 = 8;
	public static final int QUERY_9 = 9;
	public static final int QUERY_10 = 10;
	public static final int QUERY_11 = 11;
	public static final int QUERY_12 = 12;
	public static final int QUERY_13 = 13;
	public static final int QUERY_14 = 14;
	public static final int QUERY_15 = 15;
	public static final int QUERY_16 = 16;
	public static final int QUERY_17 = 17;
	public static final String DESCRIPTION_1 = "DESCRIZIONE QUERY 1:\n\nDato l'ID di una persona estrazione della traiettoria percorsa nella scena.";
	public static final String DESCRIPTION_2 = "DESCRIZIONE QUERY 2:\n\nRestituzione delle persone o dei gruppi che transitano in una certa area.";
	public static final String DESCRIPTION_3 = "DESCRIZIONE QUERY 3:\n\nRestituzione delle persone che si sono incontrate (per più di 1 s).";
	public static final String DESCRIPTION_4 = "DESCRIZIONE QUERY 4:\n\nRestituzione delle persone che sono rimaste ferme nella scena per più di un dato numero di secondi.";
	public static final String DESCRIPTION_5 = "DESCRIZIONE QUERY 5:\n\nDato l'ID di una persona e una soglia temporale restituzione delle persone che sono state presenti nella scena per un tempo superiore alla soglia.\n- Indicando come ID \"-1\" la query è eseguita su tutte le persone;\n- Indicando come soglia temporale \"-1\" la query è eseguita senza tener conto della soglia;\n";
	public static final String DESCRIPTION_6 = "DESCRIZIONE QUERY 6:\n\nData una velocità in px/fr estrazione di tutte le persone che durante la loro presenza nella scena hanno mantenuto una velocità media superiore a tale soglia.\n- se come velocità limite si indica \"0\" vengono restituite tutte le persone che si sono mosse all'interno della scena, vengono cosi esclusi individui fittizi nati da split di altri individui nelle aree di occlusione;";
	public static final String DESCRIPTION_7 = "DESCRIZIONE QUERY 7:\n\nDato in input un colore dominante tra rosso, blu e nero, restituzione delle persone che hanno tale colore dominante al primo frame al quale sono state osservate nella scena.";
	public static final String DESCRIPTION_8 = "DESCRIZIONE QUERY 8:\n\nDato l'ID di una persona estrazione dei suoi cambi di direzione all'interno dela scena.";
	public static final String DESCRIPTION_9 = "DESCRIZIONE QUERY 9:\n\nRestituzione dei gruppi individuati all'interno della scena.";
	public static final String DESCRIPTION_10 = "DESCRIZIONE QUERY 10:\n\nPersone che cambiano direzione in un'area selezionata.";
	public static final String DESCRIPTION_11 = "DESCRIZIONE QUERY 11:\n\nPersone che passano più volte in una stessa area.";
	public static final String DESCRIPTION_12 = "DESCRIZIONE QUERY 12:\n\nRestituisce gli id delle persone che sostano in una specifica area, data una soglia di secondi.";
	public static final String DESCRIPTION_13 = "DESCRIZIONE QUERY 13:\n\nPersone che incontrano altre persone in una determinata area.";
	public static final String DESCRIPTION_14 = "DESCRIZIONE QUERY 14:\n\nCalcola la velocità massima in cui si muovono le persone nella scena.\n"
												+"Se supera una determinata soglia (40 PPF) e la persona in questione ha effettuato dei cambi di "
					 							+ "direzione, viene segnalata come sospetta.";
	public static final String DESCRIPTION_15 = "DESCRIZIONE QUERY 15:\n\nRilevamento di gruppi sospetti.";
	public static final String DESCRIPTION_16 = "DESCRIZIONE QUERY 16:\n\nRilevamento di persone sospette di spaccio o scambio di merce.";
	public static final String DESCRIPTION_17 = "DESCRIZIONE QUERY 17:\n\nPersone che seguono altre persone all'interno della scena.";
	
	/**
	 * Dato l'id di una persona ne estrae la traiettoria percorsa
	 * @param base
	 * @param trackingId
	 * @return
	 */
	public static ResultSetRewindable query1(VirtGraph graph, int trackingId){
						// assegna a tracking l'URI
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
						// assegna a xsd l'URI
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
						// SELECT vuole assegnare un valore a ?personId e a ?frameId
						+ "SELECT (?personId AS ?ID_Persona) (?frameId AS ?ID_Frame) ?x ?y\n"		
						+ "FROM <"+GRAPH+">\n"
						/* WHERE indica il pattern da matchare può contenere + di 1 pattern
						* e saranno considerati come in AND tra loro, quindi se 1 non trova match,
						* il risultato complessibo sarà un grafo vuoto! */
						+ "WHERE {\n"
									+ "?person a tracking:Person;\n"
									+ "	tracking:id ?personId.\n"
									+ "?blob tracking:isAssociatedWith ?person;\n"
									+ "	tracking:hasBoundingBox ?bbox.\n"
									+ "?bbox tracking:hasCenter ?center.\n"
									+ "?center tracking:x ?x;\n"
									+ "	tracking:y ?y.\n"
									+ "?frame a tracking:Frame;\n"	
									+ "	tracking:id ?frameId.\n"
									+ "?blob tracking:seenAtFrame ?frame.\n"
									+ "FILTER(xsd:integer(?personId) = xsd:integer("+trackingId+"))\n"
						+ "}\n"
						// riordina i risultati in ordine crescente/decrescente
						+ "ORDER BY xsd:integer(?frameId)\n";
		
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		//System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Restituisce gli id delle persone o dei gruppi che rientrano in una area di allarme
	 * @param graph
	 * @param type
	 * @return
	 */
	public static ResultSetRewindable query2(VirtGraph graph, boolean type){
        // Definisco la query
		String build = null;
		if (!type) {
			// per persona
			build =	"PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "SELECT (?personId AS ?ID_Persona) (MIN(?frameId) AS ?ID_Frame)\n"
					+ "FROM <"+GRAPH+">\n"
					+ "WHERE {\n"
								+ "?blob tracking:isLocatedin tracking:PassingArea1.\n"
								+ "?blob a tracking:Blob.\n"
								+ "?blob tracking:isAssociatedWith ?person.\n"
								+ "?person a tracking:Person.\n"
								+ "?person tracking:id ?personId.\n"
								+ "?blob tracking:seenAtFrame ?frame.\n"
								+ "?frame a tracking:Frame.\n"
								+ "?frame tracking:id ?frameId.\n"
					+ "}\n"
					+ "GROUP BY ?personId\n"
					+ "ORDER BY (xsd:integer(?personId))\n";
		} else {
			// per gruppo
			build =	"PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
					+ "SELECT (?groupId AS ?ID_Gruppo) (MIN(?frameId) AS ?ID_Frame)\n"
					+ "FROM <"+GRAPH+">\n"
					+ "WHERE {\n"
						
						+ "{"
							+ "SELECT ?groupId ?frameId\n"
							+ "WHERE {\n"
								+ "?blob tracking:isLocatedin tracking:PassingArea1.\n"
								+ "?blob a tracking:Blob.\n"
								+ "?blob tracking:isAssociatedWith ?person.\n"
								+ "?blob tracking:seenAtFrame ?frame.\n"
								+ "?frame tracking:id ?frameId.\n"
								+ "?person a tracking:Person.\n"
								+ "?person ^foaf:member ?group.\n"
								+ "?person tracking:id ?personId.\n"
								+ "?group tracking:id ?groupId.\n"
								+ "?group tracking:groupSince ?frameOfGroup.\n"
								+ "FILTER (xsd:integer(?frameOfGroup)>xsd:integer(7))\n"
							+ "}\n"
							+ "GROUP BY ?groupId ?frameId\n"
							+ "HAVING (COUNT(?person)>xsd:integer(1))\n"
							+ "ORDER BY xsd:integer(?groupId)\n"
						+ "}"
					+ "}\n"
					+ "GROUP BY ?groupId\n"
					+ "ORDER BY xsd:integer(?groupId)\n";
		}
		
        // Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
       	ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Restituisce le persone che si sono incontrate
	 * @param base
	 * @return
	 */
	public static ResultSetRewindable query3(VirtGraph graph, int joinSince){
		
		String build =	"PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
				+ "SELECT (?groupId AS ?ID_Gruppo) (?personId AS ?ID_Persona) (?frameId AS ?ID_Frame) \n"
				+ "FROM <"+GRAPH+">\n"
				+ "WHERE {\n"
					+ "?group a foaf:Group.\n"
					+ "?group tracking:id ?groupId.\n"
					+ "?group foaf:member ?person1, ?person2.\n"
					
					+ "?person1 a tracking:Person."
					+ "?person2 a tracking:Person."
					+ "?person1 tracking:id ?personId."
					+ "?person2 tracking:id ?personId."
					
					+ "?group tracking:firstSeenAt ?frame.\n"
					+ "?group tracking:groupSince ?groupSince."
					+ "?group tracking:groupSinceEntry ?sinceEntry."
					+ "?frame a tracking:Frame.\n" 
					+ "?frame tracking:id ?frameId.\n"
					+ "FILTER (?sinceEntry = 'false')\n"
				+ "}\n"
				+ "GROUP BY ?groupId ?personId ?frameId \n"
				+ "HAVING (xsd:integer(?groupSince)>xsd:integer("+joinSince+"))\n"
				+ "ORDER BY xsd:integer(?groupId)\n";
		
		// Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
       	ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Trova le persone ferme per almeno un certo numero di secondi
	 * @param base
	 * @return
	 */
	public static ResultSetRewindable query4(VirtGraph graph){
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"
				+ "SELECT (?personId AS ?ID_Persona) (?frameId AS ?ID_Frame) ?direction \n"
				+ "FROM <"+GRAPH+">\n"
				+ "WHERE {\n"
							+ "?frame a tracking:Frame.\n"	
							+ "?frame tracking:id ?frameId.\n"								
							+ "?person a tracking:Person.\n"
							+ "?person tracking:id ?personId.\n"
							+ "?blob tracking:seenAtFrame ?frame.\n"
							+ "?blob tracking:hasDirection ?direction.\n"
							+ "?direction rdfs:label \"stopped\"."
		       				+ "?blob tracking:isAssociatedWith ?person.\n"
				+ "}\n"
				+ "ORDER BY xsd:integer(?frameId)";

		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Restituisce il tempo di permanenza di ogni persona nella scena, è possibile
	 * attraverso i due parametri filtrare per persona oppure porre una soglia temporale
	 * @param base
	 * @param trackingId
	 * @param time
	 * @return
	 */

	public static ResultSetRewindable query5(VirtGraph graph, int trackingId, float time){
		String filterOrNot="";

		if(trackingId!=-1)
			filterOrNot="FILTER(xsd:integer(?personId) = xsd:integer("+trackingId+"))\n";

		String build=
				"PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
						+ "SELECT (?personId AS ?ID_Persona) (MIN(xsd:integer(?frameId)) AS ?ID_Frame) (MAX(xsd:float(?time)) AS ?Time)\n"
						+ "FROM <"+GRAPH+">\n"
						+ "WHERE {\n"
						+ "?person a tracking:Person.\n"
						+ "?person tracking:id ?personId.\n"
						+ "?blob a tracking:Blob.\n"
						+ "?blob tracking:isAssociatedWith ?person.\n"
						+ "?blob tracking:seenAtFrame ?frame.\n"
						+ "?blob tracking:timeOnScene ?time.\n"
						+ "?frame a tracking:Frame.\n"
						+ "?frame tracking:id ?frameId.\n"
						+ "?blob tracking:seenAtFrame ?frame.\n"
						+ filterOrNot
						+ "}\n"
						+ "GROUP BY (?personId)\n"
						+ "HAVING (MAX(xsd:float(?time)) > xsd:float("+time+"))"
						+ "ORDER BY (xsd:integer(?personId))\n";

		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Calcola la velocità  media in cui si muovono le persone nella scena
	 * @param base
	 * @param speedTreshold
	 * @return
	 */
	public static ResultSetRewindable query6(VirtGraph graph, float speedThreshold){
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
						+ "SELECT (?personId AS ?ID_Persona) (MIN(xsd:integer(?frameId)) AS ?ID_Frame) (AVG(xsd:float(?speed)) AS ?AverageSpeed)\n"
						+ "FROM <"+GRAPH+">\n"
						+ "WHERE {\n"
									+ "?person a tracking:Person.\n"
									+ "?person tracking:id ?personId.\n"
									+ "?blob a tracking:Blob.\n"
									+ "?blob tracking:isAssociatedWith ?person.\n"
									+ "?blob tracking:seenAtFrame ?frame.\n"
									+ "?blob tracking:hasSpeed ?speed.\n"
									+ "?frame a tracking:Frame.\n"
									+ "?frame tracking:id ?frameId.\n"
						+ "} \n"
						+ "GROUP BY (?personId)\n"
						+ "HAVING (AVG(xsd:float(?speed))>xsd:float("+speedThreshold+"))\n"
						+ "ORDER BY (xsd:integer(?personId))\n";

		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	
	/**
	 * Restituisce le persone, con un certo colore dominante e il frame al quale sono state viste la prima volta
	 * @param base
	 * @param color
	 * @return
	 */
	public static ResultSetRewindable query7(VirtGraph graph, String color){
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
						+ "SELECT (?personId AS ?ID_Persona) (?frameId AS ?ID_Frame)\n"
						+ "FROM <"+GRAPH+">\n"
						+ "WHERE {\n"
									+ "?person tracking:hasDominatColor tracking:"+color+".\n"
									+ "?person a tracking:Person.\n"
									+ "?person tracking:id ?personId.\n"
									+ "?person tracking:firstSeenAt ?frame.\n"
									+ "?frame a tracking:Frame.\n"
									+ "?frame tracking:id ?frameId.\n"
						+ "} \n"
						+ "ORDER BY (xsd:integer(?personId))\n";
		
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Restituisce le persone, con un certo colore dominante e il frame al quale sono state viste la prima volta
	 * @param base
	 * @param color
	 * @return
	 */
	public static ResultSetRewindable query8(VirtGraph graph, int trackingId){

		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n "
				+ "SELECT (xsd:integer(?personId) AS ?ID_Persona) (MIN(xsd:integer(?frameId)) AS ?ID_Frame) (xsd:integer(?dirChanges) AS ?NumDirectionChanges)\n"      
				+ "FROM <"+GRAPH+">\n"
				+ "WHERE {\n"
							+ "?person a tracking:Person.\n"
							+ "?person tracking:id ?personId.\n"
							+ "?blob tracking:isAssociatedWith ?person.\n"
							+ "?blob a tracking:Blob.\n"
							+ "?blob tracking:seenAtFrame ?frame.\n"
							+ "?blob tracking:directionChanges ?dirChanges.\n"
							+ "?frame a tracking:Frame.\n" 
							+ "?frame tracking:id ?frameId.\n"
							+ "FILTER(xsd:integer(?personId) = xsd:integer("+trackingId+"))\n"
				+ "}\n"
				+ "GROUP BY ?personId ?ID_Frame ?dirChanges\n"
				+ "ORDER BY xsd:integer(?ID_Frame)\n";


		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	public static ResultSetRewindable query9(VirtGraph graph, int timeGroupSince){
		
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n "
				+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
				+ "SELECT DISTINCT (xsd:integer(?groupId) AS ?ID_Gruppo) (xsd:integer(?frameId) AS ?ID_Frame) (xsd:integer(?groupSince) AS ?Tempo_Raggruppamento)\n"
				+ "FROM <"+GRAPH+">\n"
				+ "WHERE {\n"
							+ "?group a foaf:Group.\n"
							+ "?group tracking:id ?groupId.\n"
							+ "?group foaf:member ?person1, ?person2.\n"
							
							+ "?person1 a tracking:Person."
							+ "?person2 a tracking:Person."
							+ "?person1 tracking:id ?personId."
							+ "?person2 tracking:id ?personId."
							
							+ "?group tracking:firstSeenAt ?frame.\n"
							+ "?group tracking:groupSince ?groupSince."
							+ "?frame a tracking:Frame.\n" 
							+ "?frame tracking:id ?frameId.\n"
				+ "}\n"
				+ "GROUP BY ?groupId ?frameId ?groupSince\n"
				+ "HAVING (xsd:integer(?groupSince)>xsd:integer("+timeGroupSince+"))\n"
				+ "ORDER BY xsd:integer(?groupId)\n";
				
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Cambi di direzione di una persona in un'area selezionata.
	 * @param base
	 * @param speedTreshold
	 * @return
	 */
	public static ResultSetRewindable query10(VirtGraph graph, int trackingId){
		String build = null;
		if(trackingId != 0){
				build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n "
					+ "SELECT (xsd:integer(?personId) AS ?ID_Persona) (MIN(xsd:integer(?frameId)) AS ?ID_Frame) (xsd:integer(?dirChanges) AS ?NumDirectionChanges)\n"      
					+ "FROM <"+GRAPH+">\n"
					+ "WHERE {\n"
								+ "?person a tracking:Person.\n"
								+ "?person tracking:id ?personId.\n"
								+ "?blob tracking:isAssociatedWith ?person.\n"
								+ "?blob a tracking:Blob.\n"
								+ "?blob tracking:seenAtFrame ?frame.\n"
								+ "?blob tracking:directionChanges ?dirChanges.\n"
								+ "?frame a tracking:Frame.\n" 
								+ "?frame tracking:id ?frameId.\n"
								+ "FILTER(xsd:integer(?personId) = xsd:integer("+trackingId+"))\n"
								+ "?blob tracking:isLocatedin tracking:PassingArea1.\n"
								
					+ "}\n"
					+ "GROUP BY ?personId ?dirChanges ?ID_Frame\n"
					+ "ORDER BY xsd:integer(?ID_Frame)\n";
		} else {
			build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n "
				+ "SELECT (xsd:integer(?personId) AS ?ID_Persona) (MIN(xsd:integer(?frameId)) AS ?ID_Frame) (xsd:integer(?dirChanges) AS ?NumDirectionChanges)\n"      
				+ "FROM <"+GRAPH+">\n"
				+ "WHERE {\n"
							+ "?person a tracking:Person.\n"
							+ "?person tracking:id ?personId.\n"
							+ "?blob tracking:isAssociatedWith ?person.\n"
							+ "?blob a tracking:Blob.\n"
							+ "?blob tracking:seenAtFrame ?frame.\n"
							+ "?blob tracking:directionChanges ?dirChanges.\n"
							+ "?frame a tracking:Frame.\n" 
							+ "?frame tracking:id ?frameId.\n"
							+ "?blob tracking:isLocatedin tracking:PassingArea1.\n"
				+ "}\n"
				+ "GROUP BY ?personId ?dirChanges ?ID_Frame\n"
				+ "ORDER BY xsd:integer(?ID_Frame)\n";
		}

		
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Restituisce gli id delle persone che passano più volte in una stessa area.
	 * @param base
	 * @param type
	 * @return
	 */
	public static ResultSetRewindable query11(VirtGraph graph, boolean type){
        // Definisco la query
		String build =	"PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "SELECT (?personId AS ?ID_Persona) (MAX(?frameId) AS ?ID_Frame)\n"
					+ "FROM <"+GRAPH+">\n"
					+ "WHERE {\n"
								+ "?blob tracking:isLocatedin tracking:PassingArea1.\n"
								+ "?blob a tracking:Blob.\n"
								+ "?blob tracking:isAssociatedWith ?person.\n"
								+ "?person a tracking:Person.\n"
								+ "?person tracking:id ?personId.\n"
								+ "?blob tracking:seenAtFrame ?frame.\n"
								+ "?frame a tracking:Frame.\n"
								+ "?frame tracking:id ?frameId.\n"
					+ "}\n"
					+ "GROUP BY ?personId\n"
					+ "HAVING (COUNT(?frameId)>xsd:integer(49))\n"
					+ "ORDER BY (xsd:integer(?personId))\n";
		
        // Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
       	ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	/**
	 * Restituisce gli id delle persone che sostano in una specifica area.
	 * @param base
	 * @return
	 */
	public static ResultSetRewindable query12(VirtGraph graph){
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"
				+ "SELECT (?personId AS ?ID_Persona) (?frameId AS ?ID_Frame) ?direction \n"						
				+ "FROM <"+GRAPH+">\n"
				+ "WHERE {\n"
							+ "?frame a tracking:Frame.\n"
							+ "?frame tracking:id ?frameId.\n"
							+ "?person a tracking:Person.\n"
							+ "?person tracking:id ?personId.\n"

							+ "?blob tracking:seenAtFrame ?frame.\n"
							+ "?blob tracking:hasDirection ?direction.\n"
							+ "?direction rdfs:label \"stopped\"."
		       				+ "?blob tracking:isAssociatedWith ?person.\n"
							+ "?blob tracking:isLocatedin tracking:PassingArea1.\n"
				+ "}\n"
				+ "ORDER BY xsd:integer(?frameId)";

		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
		
	}
	/**
	 * Restituisce le persone che si incontrano in una determinata area
	 * Le persone che si incontrano sono quelle che creano un gruppo, 
	 * il quale resta gruppo per almeno 28 frame.
	 * @param base
	 * @return
	 */
	public static ResultSetRewindable query13(VirtGraph graph, boolean type){
		// Definisco la query
				String build =	"PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
							+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
							+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
							+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
							+ "SELECT (?groupId AS ?ID_Gruppo) (MIN(?frameId) AS ?ID_Frame)\n"
							+ "FROM <"+GRAPH+">\n"
							+ "WHERE {\n"
										+ "?blob tracking:isLocatedin tracking:OccludingArea1.\n"
										+ "?blob a tracking:Blob.\n"
										+ "?blob tracking:isAssociatedWith ?person.\n"
										+ "?blob tracking:seenAtFrame ?frame.\n"
										+ "?frame tracking:id ?frameId.\n"
										+ "?person a tracking:Person.\n"
										+ "?person ^foaf:member ?group.\n"
										+ "?person tracking:id ?personId.\n"
										+ "?group tracking:id ?groupId.\n"
										+ "?group tracking:groupSince ?frameOfGroup.\n"
										+ "FILTER (xsd:integer(?frameOfGroup)>xsd:integer(28))\n"
							+ "}\n"
							+ "GROUP BY ?groupId\n"
							+ "ORDER BY xsd:integer(?groupId)\n";
				
		        // Esegue la QUERY
				Query sparql = QueryFactory.create(build);
				VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
				ResultSet results = vqe.execSelect();
		       	ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
				vqe.close();
				//ResultSetFormatter.out(System.out, rewindableResults);
				System.out.println(build);
				return rewindableResults;
		
	}
	
	/**
	 * Calcola la velocità  massima in cui si muovono le persone nella scena, se supera
	 * una determinata soglia e la persona in questione ha effettuato dei cambi di direzione, 
	 * la persona viene segnalata come sospetta.
	 * @param base
	 * @param speedTreshold
	 * @return
	 */

	public static ResultSetRewindable query14(VirtGraph graph, float speedTreshold){

		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
						+ "SELECT (?personId AS ?ID_Persona) (MIN(xsd:integer(?frameId)) AS ?ID_Frame) (MAX(xsd:float(?speed)) AS ?MaxSpeed)\n"						
						+ "FROM <"+GRAPH+">\n"
						+ "WHERE {\n"
									+ "?person a tracking:Person.\n"
									+ "?person tracking:id ?personId.\n"
									+ "?blob a tracking:Blob.\n"
									+ "?blob tracking:isAssociatedWith ?person.\n"
									+ "?blob tracking:seenAtFrame ?frame.\n"
									+ "?blob tracking:hasSpeed ?speed.\n"
									+ "?blob tracking:directionChanges ?dirChanges.\n"
									+ "?frame a tracking:Frame.\n"
									+ "?frame tracking:id ?frameId.\n"
						+ "} \n"
						+ "GROUP BY (?personId)\n"
						+ "HAVING (MAX(xsd:float(?speed))>xsd:float("+speedTreshold+"))\n"
						+ "ORDER BY (xsd:integer(?personId))\n";

		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	public static ResultSetRewindable query15(VirtGraph graph){
		// Definisco la query
				String build =	"PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
							+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
							+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
							+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
							+ "SELECT (?groupId AS ?ID_Gruppo) (MIN(?frameId) AS ?ID_Frame)\n"
							+ "FROM <"+GRAPH+">\n"
							+ "WHERE {\n"
									+ "SELECT ?groupId ?frameId\n"
									+ "WHERE {\n"
										+ "?blob a tracking:Blob.\n"
										+ "?blob tracking:isAssociatedWith ?person.\n"
										+ "?blob tracking:seenAtFrame ?frame.\n"
										+ "?frame tracking:id ?frameId.\n"
										+ "?person a tracking:Person.\n"
										+ "?person ^foaf:member ?group.\n"
										+ "?person tracking:id ?personId.\n"
										+ "?group tracking:id ?groupId.\n"
										+ "?group tracking:groupSince ?frameOfGroup.\n"
										// gruppo da almeno 6 sec
										+ "FILTER (xsd:integer(?frameOfGroup)>xsd:integer(42))\n"
									+ "}\n"
									+ "GROUP BY ?groupId ?frameId\n"
									+ "HAVING (COUNT(?person)>xsd:integer(1))\n"
									+ "ORDER BY xsd:integer(?groupId)\n"
							+ "}\n"
							+ "GROUP BY ?groupId\n"
							+ "ORDER BY xsd:integer(?groupId)\n";
				
		        // Esegue la QUERY
				Query sparql = QueryFactory.create(build);
				VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
				ResultSet results = vqe.execSelect();
		       	ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
				vqe.close();
				//ResultSetFormatter.out(System.out, rewindableResults);
				System.out.println(build);
				return rewindableResults;
			
	}
	
	/**
	 * Restituisce le persone che si sono incontrate quando una era ferma nella scena
	 * @param base
	 * @return
	 */
	public static ResultSetRewindable query16(VirtGraph graph, boolean type){
		// Definisco la query
		String build ="PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n"
		+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
		+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
		+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"

		+ "SELECT (?groupId AS ?ID_Gruppo) (MIN(?frameId) AS ?ID_Frame)\n"
		+ "FROM <"+GRAPH+">\n"
		+ "WHERE {\n"
		+ "?frame a tracking:Frame.\n"
		+ "?frame tracking:id ?frameId.\n"
		+ "?person a tracking:Person.\n"
		+ "?person tracking:id ?personId.\n"

		+ "?blob tracking:seenAtFrame ?frame.\n"
		+ "?blob tracking:hasDirection ?direction.\n"
		+ "?direction rdfs:label \"stopped\"."
		+ "?blob tracking:isAssociatedWith ?person.\n"

		+ "{"
		+ "SELECT ?groupId ?frameId\n"
		+ "WHERE {\n"
		+ "?blob tracking:isLocatedin tracking:OccludingArea1.\n"
		+ "?blob a tracking:Blob.\n"
		+ "?blob tracking:isAssociatedWith ?person.\n"
		+ "?blob tracking:seenAtFrame ?frame.\n"
		+ "?frame tracking:id ?frameId.\n"
		+ "?person a tracking:Person.\n"
		+ "?person ^foaf:member ?group.\n"
		+ "?person tracking:id ?personId.\n"
		+ "?group tracking:id ?groupId.\n"
		+ "?group tracking:groupSince ?frameOfGroup.\n"
		+ "FILTER (xsd:integer(?frameOfGroup)>xsd:integer(28))\n"
		+ "}\n"
		+ "GROUP BY ?groupId ?frameId\n"
		+ "HAVING (COUNT(?person)>xsd:integer(1))\n"
		+ "ORDER BY xsd:integer(?groupId)\n"
		+ "}"
		+ "}\n"
		+ "GROUP BY ?groupId\n"
		+ "ORDER BY xsd:integer(?groupId)\n";

		// Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
	}
	
	public static ResultSetRewindable query17(VirtGraph graph){
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n "
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n "
						+ "PREFIX bif:<bif:>\n"
						+ "SELECT ?tlv1 ?tlv2 ?person1 ?person2 ?frameId ?personId1 ?personId2\n "
						+ "FROM <"+GRAPH+">\n"
						+ "WHERE{\n"
									
									+ "?frame tracking:id ?frameId. \n"
									+ "?frame a tracking:Frame. \n"
									
									+ "?blob1 tracking:isAssociatedWith ?person1. \n"
									+ "?blob1 a tracking:Blob. \n"
									+ "?blob1 tracking:hasBoundingBox ?boundingBox1. \n"
			
									+ "?blob2 tracking:isAssociatedWith ?person2. \n"
									+ "?blob2 a tracking:Blob. \n"
									+ "?blob2 tracking:hasBoundingBox ?boundingBox2. \n"
																		
									+ "?person1 tracking:id ?personId1.\n"
									+ "?person1 a tracking:Person. \n"
									
									+ "?person2 tracking:id ?personId2.\n"
									+ "?person2 a tracking:Person. \n"

									+ "?boundingBox1 tracking:hasCenter ?cp1. \n"
									+ "?boundingBox2 tracking:hasCenter ?cp2. \n"
									
									+ "?cp1 tracking:x ?cp1_x. \n"
									+ "?cp1 tracking:y ?cp1_y. \n"
									+ "?cp2 tracking:x ?cp2_x. \n"
									+ "?cp2 tracking:y ?cp2_y. \n"
									//FILTER per differenza di center points (dei bounding box) di almeno 1 mt 
									+ "FILTER ((bif:sqrt(bif:power((xsd:integer(?cp2_x)) - (xsd:integer(?cp1_x)),2) + bif:power((xsd:integer(?cp2_y)) - (xsd:integer(?cp1_y)),2))) < xsd:integer(3779))\n"
									+ "}"
									+ "ORDER BY (xsd:integer(?frameId))\n";
		
		// Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		System.out.println(build);
		return rewindableResults;
		 
	}
		
	public static Rectangle getPersonAtFrameRectangle(VirtGraph graph, int personId, int frameId){
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n "
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n "
						+ "SELECT ?tlv_x ?tlv_y ?brv_x ?brv_y \n "
						+ "FROM <"+GRAPH+">\n"
						+ "WHERE{\n"
									+ "?person tracking:id \""+personId+"\".\n"
									+ "?person a tracking:Person. \n"
									
									+ "?frame tracking:id \""+frameId+"\". \n"
									+ "?frame a tracking:Frame. \n"
									
									+ "?blob tracking:seenAtFrame ?frame. \n"
									+ "?blob tracking:isAssociatedWith ?person. \n"
									+ "?blob a tracking:Blob. \n"
									
									+ "?blob tracking:hasBoundingBox ?boundingBox. \n"
									+ "?boundingBox tracking:topLeftVertex ?tlv. \n"
									+ "?boundingBox tracking:bottomRightVertex ?brv. \n"
			
									+ "?tlv tracking:x ?tlv_x. \n"
									+ "?tlv tracking:y ?tlv_y. \n"
									+ "?brv tracking:x ?brv_x. \n"
									+ "?brv tracking:y ?brv_y. \n"
						+ "}";
		
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();
		if(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
		
			int tlv_x = sol.get("tlv_x").asLiteral().getInt();
			int tlv_y = sol.get("tlv_y").asLiteral().getInt();
			int brv_x = sol.get("brv_x").asLiteral().getInt();
			int brv_y = sol.get("brv_y").asLiteral().getInt();
		
			int width = brv_x-tlv_x;
			int height = brv_y-tlv_y;
		
			return new Rectangle(tlv_x, tlv_y, width, height);
		} else
			return null;
	}
		
	public static ResultSetRewindable getPersonMemberOfAGroup(VirtGraph graph, int groupId) {//, int frameId){
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n "
						+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
						+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT (?personId AS ?ID_Persona)\n"
						+ "FROM <"+GRAPH+">\n"
						+ "WHERE {\n"
									+ "?group tracking:id \""+groupId+"\".\n"
									+ "?group a foaf:Group.\n"
									
									+ "?group foaf:member ?person.\n"
									+ "?person a tracking:Person.\n"
									+ "?person tracking:id ?personId.\n"
						+ "}\n"
						+ "ORDER BY xsd:integer(?personId)";
		
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		ResultSet results = vqe.execSelect();

		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		
		return rewindableResults;
	}

	public static ArrayList<Rectangle> getPersonMemberOfAGroupRectangleAtAFrame(VirtGraph graph, int groupId, int frameId){
		ResultSetRewindable results = getPersonMemberOfAGroup(graph, groupId);
		
		QuerySolution sol;
		int personId;
		ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>(); 
		
		results.reset();
		while(results.hasNext()){
			sol = results.nextSolution();
			personId = sol.get("ID_Persona").asLiteral().getInt();
			rectangles.add(getPersonAtFrameRectangle(graph, personId, frameId));
		}
		return rectangles;
	}
	
	public static ResultSetRewindable getFootCenterAndDir(VirtGraph graph, String frame, String person) {
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n " + 
	                   "PREFIX bif:<bif:> \n" +
	                   "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
					   "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
				"SELECT ?dirLab ?x ?y \n" + 
				"FROM <" + GRAPH + "> \n" + 
				"WHERE\n" + 
				"{\n" + 
				"   #Ottengo il blob dalla persona ad un determinato frame\n" + 
				"   ?blob tracking:seenAtFrame " + "<" + frame + ">" + ".\n" + 
				"   " + "<" + person + ">" + " tracking:blobMatch ?blob .\n" + 
				"   \n" + 
				"   #Ottengo la direzione e il BoundingBox della persona\n" + 
				"   ?blob tracking:hasDirection ?dir ;\n" + 
				"          tracking:hasBoundingBox ?bb .\n" + 
				"   ?dir <http://www.w3.org/2000/01/rdf-schema#label> ?dirLab .\n" +
				"   \n" + 
				"   #Ottengo le coordinate del centro dei piedi della persona\n" + 
				"   ?bb tracking:bottomLeftVertex ?blv ;\n" + 
				"        tracking:bottomRightVertex ?brv .\n" + 
				"   \n" + 
				"   #Ottengo le ascisse dei punti\n" + 
				"   ?blv tracking:x ?blv_x ;\n" + 
				"        tracking:y ?blv_y .\n" + 
				"   ?brv tracking:x ?brv_x .\n" + 
				"\n" + 
				"   #Coordinate centro piedi persona\n" + 
				"   BIND(xsd:integer(bif:round((xsd:integer(?blv_x) + xsd:integer(?brv_x))/2)) AS ?x) .\n" + 
				"   BIND(xsd:integer(?blv_y) AS ?y) .\n" + 
				"}";
		
		// Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		return rewindableResults;
	}
	
	public static ResultSetRewindable areSameDirections(VirtGraph graph, String dir1, String dir2) {
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n " + 
	                   "PREFIX bif:<bif:> \n" +
	                   "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
					   "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
				"SELECT ?res\r\n" +
				"FROM <" + GRAPH + "> \n" +
				"WHERE{\r\n" + 
				"   BIND(xsd:string(\"north\") AS ?north) .\n" + 
				"   BIND(xsd:string(\"nest\") AS ?nest) .\n" + 
				"   BIND(xsd:string(\"nwest\") AS ?nwest) .\n" + 
				"   BIND(xsd:string(\"south\") AS ?south) .\n" + 
				"   BIND(xsd:string(\"sest\") AS ?sest) .\n" + 
				"   BIND(xsd:string(\"swest\") AS ?swest) .\n" + 
				"   BIND(xsd:string(\"est\") AS ?est) .\n" + 
				"   BIND(xsd:string(\"west\") AS ?west) .\n" + 
				"   BIND(xsd:string(\"stopped\") AS ?stopped) .\n" + 
				"\n" + 
				"   BIND(xsd:string(\"" + dir1 + "\") AS ?dir1) .\r\n" + 
				"   BIND(xsd:string(\"" + dir2 + "\") AS ?dir2) .\r\n" + 
				"\r\n" + 
				"   BIND((\r\n" + 
				"   IF(?dir1=?north, (?dir2=?nest || ?dir2=?nwest || ?dir2=?north), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?south, (?dir2=?sest || ?dir2=?south || ?dir2=?swest), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?nest, (?dir2=?north || ?dir2=?nest || ?dir2=?est), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?est, (?dir2=?sest || ?dir2=?nest || ?dir2=?est), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?sest, (?dir2=?south || ?dir2=?est || ?dir2=?sest), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?swest, (?dir2=?south || ?dir2=?swest || ?dir2=?west), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?west, (?dir2=?swest || ?dir2=?west || ?dir2=?nwest), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?nwest, (?dir2=?west || ?dir2=?nwest || ?dir2=?north), xsd:integer(0))\r\n" + 
				"   ||\r\n" + 
				"   IF(?dir1=?stopped, xsd:integer(1), xsd:integer(0))\r\n" + 
				"   ) AS ?res)\r\n" + 
				"}";
		
		// Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		return rewindableResults;
	}
	
	public static void updateGroups(VirtGraph graph, String frame, String person1, String person2) {
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n " + 
	                   "PREFIX bif:<bif:> \n" +
	                   "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
					   "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
				"WITH <" + GRAPH + ">\r\n" + 
				"DELETE \r\n" + 
				"{\r\n" + 
				"   ?group tracking:lastSeenAt ?lastSeenFrame;\r\n" + 
				"          tracking:groupSince ?oldGroupSince.\r\n" + 
				"\r\n" + 
				"}\r\n" + 
				"INSERT \r\n" + 
				"{\r\n" + 
				"   ?group tracking:lastSeenAt ?currentFrame;\r\n" + 
				"          tracking:groupSince ?groupSince.\r\n" + 
				"}\r\n" + 
				"WHERE\r\n" + 
				"{\r\n" + 
				"   BIND(xsd:integer(14) AS ?oldGroupThreshold) .\r\n" + 
				"   BIND(" + "<" + frame + ">" + " AS ?currentFrame) .\r\n" + 
				"\r\n" + 
                "<"+person1+"> a tracking:Person .\n" +
				"<"+person2+"> a tracking:Person .\n" + 
				"  ?group a foaf:Group;\r\n" + 
				"          foaf:member " + "<" + person1 + ">" + "; \n" +
				"          foaf:member " + "<" + person2 + ">" + "; \n" +
				"          tracking:lastSeenAt ?lastSeenFrame;\r\n" + 
				"          tracking:groupSince ?oldGroupSince .\r\n" + 
				"\r\n" + 
				"  ?lastSeenFrame tracking:id ?lastSeenFrameId .\r\n" + 
				"  ?currentFrame tracking:id ?currentFrameId .\r\n" + 
				"  BIND((xsd:integer(?currentFrameId) - xsd:integer(?lastSeenFrameId)) AS ?groupOldness) .\r\n" + 
				"   \r\n" + 
				"  ?firstSeenFrame ^tracking:firstSeenAt ?group ;\r\n" + 
				"                   tracking:id ?firstSeenFrameId .\r\n" + 
				"\r\n" + 
				"  BIND(STR(xsd:integer(?lastSeenFrameId) - xsd:integer(?firstSeenFrameId) + 1) AS ?groupSince) . \r\n" + 
				"  \r\n" + 
				"  FILTER(?groupOldness <= ?oldGroupThreshold)\r\n" + 
				"}\r\n" + 
				"";
		
		// Esegue la QUERY	
		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(build, graph);
		vur.exec();
	}
	
	public static void insertGroups(VirtGraph graph, String group, String frame, String person1, String person2, int groupId) {
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n " + 
	                   "PREFIX bif:<bif:> \n" +
	                   "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
					   "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
					   "WITH <" + GRAPH + ">\r\n" + 
					   "INSERT{\r\n" + 
					   "     foaf:" + group +  " a foaf:Group ;\r\n" + 
					   "                      foaf:member " + "<" + person1 + ">" + ";\r\n" + 
					   "                      foaf:member " + "<" + person2 + ">" + ";\r\n" + 
					   "                      tracking:id '"+ groupId + "' ;\r\n" + 
					   "                      tracking:firstSeenAt " + "<" + frame + ">" + " ;\r\n" + 
					   "                      tracking:lastSeenAt " + "<" + frame + ">" + " ;\r\n" + 
					   "                      tracking:groupSince \"1\";\r\n" + 
					   "					  tracking:groupSinceEntry ?isProbAGroup. \r\n" + 
					   "}\r\n" + 
					   "WHERE{  \r\n" + 
					   "<" + person1 + ">" + "tracking:isProbablyAGroup ?isProbablyAGroup1 .\r\n" + 
					   "<" + person2 + ">" + "tracking:isProbablyAGroup ?isProbablyAGroup2 .\r\n" +
					   "BIND(IF((?isProbablyAGroup1 = \"" + "true" + "\") && (?isProbablyAGroup2 = \""+ "true" + "\"), \"" + "true" + "\", \"" + "false" + "\")" + " AS ?isProbAGroup) . \n" +
					   "  \r\n" + 
					   "  FILTER ((\r\n" + 
					   "  SELECT (COUNT(?group) AS ?numGroups)\r\n" + 
					   "  WHERE {\r\n" + 
					   "      BIND(xsd:integer(14) AS ?oldGroupThreshold) .\r\n" + 
					   "      BIND(" + "<" + frame + ">" + " AS ?currentFrame) .\r\n" + 
					   "\r\n" + 
					   "      ?group a foaf:Group;\r\n" + 
					   "            foaf:member " + "<" + person1 + ">"+ "," + "<" + person2 + ">" + ";\r\n" + 
					   "            tracking:lastSeenAt ?lastSeenFrame .\r\n" + 
					   "		  \r\n" + 
					   "      ?lastSeenFrame tracking:id ?lastSeenFrameId .\r\n" + 
					   "      ?currentFrame tracking:id ?currentFrameId .\r\n" + 
					   "      BIND((xsd:integer(?currentFrameId) - xsd:integer(?lastSeenFrameId)) AS ?groupOldness) .	\r\n" + 
					   "      FILTER(?groupOldness <= ?oldGroupThreshold)    	\r\n" + 
					   "  }\r\n" + 
					   "  ) = 0) \r\n" + 
					   "}";
		// Esegue la QUERY
		System.out.println(build);
		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(build, graph);
		vur.exec();
	}
	
	public static ResultSetRewindable countGroups(VirtGraph graph, String frame, String person1, String person2) {
		String build = "PREFIX tracking:<http://mivia.unisa.it/videotracking/tracking.owl#>\n " + 
                "PREFIX bif:<bif:> \n" +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
				"PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
				"SELECT (COUNT(?group) AS ?numGroups)\r\n" + 
				"				  WHERE {\r\n" + 
				"				      BIND(xsd:integer(14) AS ?oldGroupThreshold) .\r\n" + 
				"				      BIND(" + "<" + frame + ">" + "AS ?currentFrame) .\r\n" + 
				"\r\n" + 
                "					  <"+person1+"> a tracking:Person .\n" +
				"					  <"+person2+"> a tracking:Person .\n" + 
				"				      ?group a foaf:Group;\r\n" + 
				"				            foaf:member " + "<" + person1 + ">" + ", " + "<" + person2 + ">" + ";\r\n" + 
				"				            tracking:lastSeenAt ?lastSeenFrame .\r\n" + 
				"						  \r\n" + 
				"				      ?lastSeenFrame tracking:id ?lastSeenFrameId .\r\n" + 
				"				      ?currentFrame tracking:id ?currentFrameId .\r\n" + 
				"				      BIND((xsd:integer(?currentFrameId) - xsd:integer(?lastSeenFrameId)) AS ?groupOldness) .	\r\n" + 
				"				      FILTER(?groupOldness <= ?oldGroupThreshold)    	\r\n" + 
				"				  }\r\n";
		// Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		return rewindableResults;
	}
	
	public static ResultSetRewindable getSpeed(VirtGraph graph, Point prec, Point post) {
		String build = "PREFIX bif:<bif:>\r\n" + 
				"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\r\n" + 
				"\r\n" + 
				"SELECT ?avgSpeed\r\n" + 
				"WHERE {\r\n" + 
				"	BIND (bif:sqrt(bif:power(("+prec.x+"-"+post.x+"),2)+bif:power(("+prec.y+"-"+post.y+"),2)) AS ?dist) .\r\n" + 
				"	BIND (xsd:float(0.1428) AS ?time) .\r\n" + 
				"	BIND ((xsd:float(?dist)/xsd:float(?time)) AS ?avgSpeed)\r\n" + 
				"}";
		// Esegue la QUERY
		Query sparql = QueryFactory.create(build);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, graph);
		ResultSet results = vqe.execSelect();
		ResultSetRewindable rewindableResults = ResultSetFactory.copyResults(results);
		vqe.close();
		//ResultSetFormatter.out(System.out, rewindableResults);
		return rewindableResults;
	}
}
