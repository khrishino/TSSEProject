package tracking;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

import virtuoso.jena.driver.VirtGraph;

public class ObjectTypeProperty {
	VirtGraph graph;
	String NS;
	Classes classes;
	DataTypeProperty dtp;
	
	public ObjectTypeProperty(VirtGraph graph, String nameSpace){
		this.graph = graph;
		this.NS = nameSpace;
		
		// Aggiunta proprietà isAssociatedWith
		Node s1 = NodeFactory.createURI(NS + "isAssociatedWith");
		Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o1 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#ObjectProperty");
		graph.add(new Triple(s1, p1, o1));

		// Definisco dominio di isAssociatedWith
		Node s2 = NodeFactory.createURI(NS + "isAssociatedWith");
		Node p2 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o2 = NodeFactory.createURI(NS + "Blob");
		graph.add(new Triple(s2, p2, o2));

		// Definisco range di isAssociatedWith
		Node s3 = NodeFactory.createURI(NS + "isAssociatedWith");
		Node p3 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o3 = NodeFactory.createURI(NS + "Person");
		graph.add(new Triple(s3, p3, o3));
		
		// Aggiunta proprietà seenAtFrame
		Node s4 = NodeFactory.createURI(NS + "seenAtFrame");
		Node p4 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o4 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#ObjectProperty");
		graph.add(new Triple(s4, p4, o4));

		// Definisco dominio di seenAtFrame
		Node s5 = NodeFactory.createURI(NS + "seenAtFrame");
		Node p5 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o5 = NodeFactory.createURI(NS + "Blob");
		graph.add(new Triple(s5, p5, o5));

		// Definisco range di seenAtFrame
		Node s6 = NodeFactory.createURI(NS + "seenAtFrame");
		Node p6 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o6 = NodeFactory.createURI(NS + "Frame");
		graph.add(new Triple(s6, p6, o6));
		
		// Aggiunta proprietà lastSeenAt
		Node s7 = NodeFactory.createURI(NS + "lastSeenAt");
		Node p7 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o7 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#ObjectProperty");
		graph.add(new Triple(s7, p7, o7));

		// Definisco dominio di lastSeenAt
		Node s8 = NodeFactory.createURI(NS + "lastSeenAt");
		Node p8 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#domain");
		Node o8 = NodeFactory.createURI(NS + "TrackedObject");
		graph.add(new Triple(s8, p8, o8));

		// Definisco range di lastSeenAt
		Node s9 = NodeFactory.createURI(NS + "lastSeenAt");
		Node p9 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#range");
		Node o9 = NodeFactory.createURI(NS + "Frame");
		graph.add(new Triple(s9, p9, o9));
	}

}