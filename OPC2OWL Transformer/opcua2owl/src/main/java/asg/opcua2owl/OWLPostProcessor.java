package asg.opcua2owl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;

public class OWLPostProcessor implements PostProcessor {
	private final String uaBaseNsRdfStyle = "http://opcfoundation.org/UA/#";
	
	@Override
	public OntModel process(Map<String, Model> models, String namespaceRdfStyle, boolean imports, Logger logger) {
		Hashtable<String, OntModel> ontModels = new Hashtable<String, OntModel>();
		for(String nsRdfStyle : models.keySet()) {
			OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			ontModel.add(models.get(nsRdfStyle));
			
			// add the <ontology rdf:about = " ... ">
			Ontology baseOnt = ontModel.createOntology(nsRdfStyle);
			
			// Import all other namespaces
			if(imports)
				for(String nsRdfStyleToBeImported : models.keySet()) {
					if(nsRdfStyle.equals(nsRdfStyleToBeImported))
						continue;
					Resource ontToBeImported = ontModel.createResource(nsRdfStyleToBeImported);
					baseOnt.addImport(ontToBeImported);
				}
			
			// set namespace prefixes
			ontModel.setNsPrefix("uaBase", uaBaseNsRdfStyle);
			ontModels.put(nsRdfStyle, ontModel);
		}
		
		// create a new model from the old one
		OntModel ontModel = ontModels.get(namespaceRdfStyle);
		OntModel uaBaseOntModel = ontModels.get(uaBaseNsRdfStyle);
		
		// create a label for all resources from their uaBase:DisplayName
		Property displayNameProperty = uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "DisplayName");
		for(ResIterator resources = ontModel.listResourcesWithProperty(displayNameProperty); resources.hasNext();) {
			Resource resource = resources.next();
			Literal displayNameLiteral = resource.getProperty(displayNameProperty).getObject().asLiteral();
			resource.addProperty(RDFS.label, displayNameLiteral.getString());
		}

		// every resource with uaBase:nodeClass == *Type, except for ReferenceType is a class
		Property nodeClassProperty = uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "NodeClass");
		for(ResIterator resources = ontModel.listResourcesWithProperty(nodeClassProperty); resources.hasNext();) {
			Resource resource = resources.next();
			Literal nodeClassLiteral = resource.getProperty(nodeClassProperty).getObject().asLiteral();
			if(nodeClassLiteral.getString().endsWith("Type") && !nodeClassLiteral.getString().equals("ReferenceType"))
				ontModel.createClass(resource.getURI());
		}

		// every resource with uaBase:HasTypeDefinition is an individual
		Property hasTypeDefinitionProperty = uaBaseOntModel.createObjectProperty(uaBaseNsRdfStyle + "HasTypeDefinition");
		for(ResIterator resources = ontModel.listResourcesWithProperty(hasTypeDefinitionProperty); resources.hasNext();) {
			Resource subject = resources.next();
			Resource object = subject.getProperty(hasTypeDefinitionProperty).getObject().asResource();
			ontModel.createIndividual(subject.getURI(), object);
		}
		
		// every resource with uaBase:hasSubtype is the superclass of another class
		Property hasSubtypeProperty = uaBaseOntModel.createObjectProperty(uaBaseNsRdfStyle + "HasSubtype");
		for(String ns : ontModels.keySet()) {
			OntModel currentModel = ontModels.get(ns);
			for(ResIterator subjectResources = currentModel.listResourcesWithProperty(hasSubtypeProperty); subjectResources.hasNext();) {
				Resource subjectResource = subjectResources.next();
				for(StmtIterator stmt = subjectResource.listProperties(hasSubtypeProperty); stmt.hasNext();) {
					Resource objectResource = stmt.next().getObject().asResource();
					if(objectResource.getNameSpace().equals(namespaceRdfStyle)) {
						OntClass objectClass = ontModel.createClass(objectResource.getURI());
						objectClass.addSuperClass(subjectResource);
						System.out.println(objectClass.toString() + "is subclass of " + subjectResource.toString());
					}
				}
			}
		}
		
		// create annotation properties, e.g. uaBase:Value for all OPC UA attributes
		if(namespaceRdfStyle.equals(uaBaseNsRdfStyle)) {
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "AccessLevel");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "ArrayDimensions");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "BrowseName");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "ContainsNoLoops");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "DataType");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "Description");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "DisplayName");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "Executable");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "EventNotifier");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "Historizing");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "InverseName");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "IsAbstract");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "MinimumSamplingInterval");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "NodeClass");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "NodeId");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "Symmetric");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "UserAccessLevel");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "UserExecutable");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "UserWriteMask");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "Value");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "ValueRank");
			uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "WriteMask");
		}
		
		// every resource with uaBase:nodeClass == ReferenceType is an object property
		// TODO: the fact that references are modeled as resources is caused by the transformation process. It may be preferable to create properties instead.
		for(ResIterator resources = ontModel.listResourcesWithProperty(nodeClassProperty); resources.hasNext();) {
			Resource resource = resources.next();
			Literal nodeClassLiteral = resource.getProperty(nodeClassProperty).getObject().asLiteral();
			if(nodeClassLiteral.getString().equals("ReferenceType"))
				ontModel.createObjectProperty(resource.getURI());
		}
		
		// create object properties, e.g. uaBase:HasComponent for all non-annotation properties and add some characteristics, e.g. symmetric. etc.
		List<OntProperty> ontProperties = ontModel.listAllOntProperties().toList();
		for(OntProperty ontProperty : ontProperties) {
			String ontPropertyNsRdfStyle = ontProperty.getNameSpace();
			OntModel ontPropertyModel = ontModels.get(ontPropertyNsRdfStyle);
			
			if(ontProperty.isAnnotationProperty())
				continue;
			
			ObjectProperty ontObjProperty = ontModel.createObjectProperty(ontProperty.getURI());
			
			// declare the property symmetric if the corresponding OPC UA reference is symmetric
			AnnotationProperty symmetricProperty = uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "Symmetric");
			if(ontObjProperty.getProperty(symmetricProperty) != null) {
				Literal symmetricLiteral = ontObjProperty.getProperty(symmetricProperty).getObject().asLiteral();
				if(symmetricLiteral.getBoolean())
					ontObjProperty.isSymmetricProperty();
			}
			
			// create an inverse property if the OPC UA reference has an inverse name
			AnnotationProperty inverseNameProperty = uaBaseOntModel.createAnnotationProperty(uaBaseNsRdfStyle + "InverseName");
			if(ontObjProperty.getProperty(inverseNameProperty) != null) {				
				Literal inverseNameLiteral = ontObjProperty.getProperty(inverseNameProperty).getObject().asLiteral();
				if(inverseNameLiteral.getString() != null) {
					ObjectProperty inverseProperty = ontPropertyModel.createObjectProperty(ontPropertyNsRdfStyle + inverseNameLiteral.getString());
					inverseProperty.addInverseOf(ontProperty);
				}
			}
		}
		
		return ontModel;
	}

	@Override
	public OntModel substitudeNamespaces(OntModel ontModel, Map<String, String> nsSubstitionsTable) {
		
		// serialize the model as RDF/XML
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ontModel.write(out, "RDF/XML");
		
		String serializedModel = out.toString();
		for(String ns1 : nsSubstitionsTable.keySet()) {
			serializedModel = serializedModel.replace("\""+ns1, "\""+nsSubstitionsTable.get(ns1));
		}
		
		InputStream is = new ByteArrayInputStream( serializedModel.getBytes() );
		OntModel newOntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		newOntModel.read(is, null);
		
		return newOntModel;
	}

}
