package tracking;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

public class DataTypeProperty {
	VirtGraph graph;
	String NS;
	private final String GRAPH = "http://localhost:8890/TSSEProject";

	public DataTypeProperty(VirtGraph graph, String nameSpace) {
		this.graph = graph;
		this.NS = nameSpace;

		// Aggiunta proprietà stopped
		Node s1 = NodeFactory.createURI(NS + "stopped");
		Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o1 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s1, p1, o1));

		// Aggiunta label a proprietà stopped
		String str = "INSERT INTO GRAPH <"+GRAPH+"> { <"+NS+"stopped> <http://www.w3.org/2000/01/rdf-schema#label> 'stopped'}";
		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(str, graph);
        vur.exec();

		// Definisco stopped come subproperty di direction
		Node s3 = NodeFactory.createURI(NS + "stopped");
		Node p3 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#subPropertyOf");
		Node o3 = NodeFactory.createURI(NS + "direction");
		graph.add(new Triple(s3, p3, o3));

		// Aggiunta proprietà hasSpeed
		Node s4 = NodeFactory.createURI(NS + "hasSpeed");
		Node p4 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o4 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s4, p4, o4));

		// Definisco dominio di hasSpeed
		Node s5 = NodeFactory.createURI(NS + "hasSpeed");
		Node p5 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o5 = NodeFactory.createURI(NS + "Blob");
		graph.add(new Triple(s5, p5, o5));

		// Definisco range di hasSpeed
		Node s6 = NodeFactory.createURI(NS + "hasSpeed");
		Node p6 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o6 = NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#string");
		graph.add(new Triple(s6, p6, o6));

		// Aggiunta proprietà isProbablyAGroup
		Node s7 = NodeFactory.createURI(NS + "isProbablyAGroup");
		Node p7 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o7 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s7, p7, o7));

		// Definisco dominio di isProbablyAGroup
		Node s8 = NodeFactory.createURI(NS + "isProbablyAGroup");
		Node p8 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o8 = NodeFactory.createURI(NS + "Blob");
		graph.add(new Triple(s8, p8, o8));

		// Aggiunta proprietà groupSinceEntry
		Node s9 = NodeFactory.createURI(NS + "groupSinceEntry");
		Node p9 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o9 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s9, p9, o9));

		// Definisco dominio di groupSinceEntry
		Node s10 = NodeFactory.createURI(NS + "groupSinceEntry");
		Node p10 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o10 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/Group");
		graph.add(new Triple(s10, p10, o10));

		// Definisco range di groupSinceEntry
		Node s11 = NodeFactory.createURI(NS + "groupSinceEntry");
		Node p11 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o11 = NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#boolean");
		graph.add(new Triple(s11, p11, o11));
		
		// Aggiunta proprietà timeOnScene
		Node s12 = NodeFactory.createURI(NS + "timeOnScene");
		Node p12 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o12 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s12, p12, o12));

		// Definisco dominio di timeOnScene
		Node s13 = NodeFactory.createURI(NS + "timeOnScene");
		Node p13 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o13 = NodeFactory.createURI(NS + "Blob");
		graph.add(new Triple(s13, p13, o13));

		// Definisco range di timeOnScene
		Node s14 = NodeFactory.createURI(NS + "timeOnScene");
		Node p14 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o14 = NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#float");
		graph.add(new Triple(s14, p14, o14));
		
		// Aggiunta proprietà directionChanges
		Node s15 = NodeFactory.createURI(NS + "directionChanges");
		Node p15 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o15 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s15, p15, o15));

		// Definisco dominio di directionChanges
		Node s16 = NodeFactory.createURI(NS + "directionChanges");
		Node p16 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o16 = NodeFactory.createURI(NS + "Person");
		graph.add(new Triple(s16, p16, o16));

		// Definisco range di directionChanges
		Node s17 = NodeFactory.createURI(NS + "directionChanges");
		Node p17 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o17 = NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#integer");
		graph.add(new Triple(s17, p17, o17));
		
		// Aggiunta proprietà sameDirectionFrames
		Node s18 = NodeFactory.createURI(NS + "sameDirectionFrames");
		Node p18 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o18 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s18, p18, o18));

		// Definisco dominio di sameDirectionFrames
		Node s19 = NodeFactory.createURI(NS + "sameDirectionFrames");
		Node p19 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o19 = NodeFactory.createURI(NS + "Blob");
		graph.add(new Triple(s19, p19, o19));

		// Definisco range di sameDirectionFrames
		Node s20 = NodeFactory.createURI(NS + "sameDirectionFrames");
		Node p20 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o20 = NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#integer");
		graph.add(new Triple(s20, p20, o20));
		
		// Aggiunta proprietà malformedBlob
		Node s21 = NodeFactory.createURI(NS + "malformedBlob");
		Node p21 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o21 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s21, p21, o21));

		// Definisco dominio di malformedBlob
		Node s22 = NodeFactory.createURI(NS + "malformedBlob");
		Node p22 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o22 = NodeFactory.createURI(NS + "Blob");
		graph.add(new Triple(s22, p22, o22));

		// Definisco range di malformedBlob
		Node s23 = NodeFactory.createURI(NS + "malformedBlob");
		Node p23 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o23 = NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#boolean");
		graph.add(new Triple(s23, p23, o23));
		
		// Aggiunta proprietà groupSince
		Node s24 = NodeFactory.createURI(NS + "groupSince");
		Node p24 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o24 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
		graph.add(new Triple(s24, p24, o24));

		// Definisco dominio di groupSince
		Node s25 = NodeFactory.createURI(NS + "groupSince");
		Node p25 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o25 = NodeFactory.createURI("http://xmlns.com/foaf/0.1/Group");
		graph.add(new Triple(s25, p25, o25));

		// Definisco range di groupSince
		Node s26 = NodeFactory.createURI(NS + "groupSince");
		Node p26 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o26 = NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#integer");
		graph.add(new Triple(s26, p26, o26));
	}
}