package tracking;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

import virtuoso.jena.driver.VirtGraph;

public class Classes {
	VirtGraph graph;
	String NS;

	public Classes(VirtGraph graph, String nameSpace) {
		this.graph = graph;
		this.NS = nameSpace;

		// aggiunte dal gruppo "Progetto 3"
		Node s1 = NodeFactory.createURI(NS + "PassingArea");
		Node p1 = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Node o1 = NodeFactory.createURI("http://www.w3.org/2002/07/owl#Class");
		graph.add(new Triple(s1, p1, o1));

		Node s2 = NodeFactory.createURI(NS + "PassingArea");
		Node p2 = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#subClassOf");
		Node o2 = NodeFactory.createURI(NS + "Area");
		graph.add(new Triple(s2, p2, o2));
	}

}
