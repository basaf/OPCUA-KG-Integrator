package asg.opcua2owl;

import org.slf4j.Logger;
import java.util.Map;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;

public interface PostProcessor {
	public OntModel process(Map<String, Model> models, String namespace, boolean imports, Logger logger);
	public OntModel substitudeNamespaces(OntModel ontModel, Map<String, String> nsSubstitutionTable);
}
