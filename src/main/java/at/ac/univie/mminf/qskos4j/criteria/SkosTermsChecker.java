package at.ac.univie.mminf.qskos4j.criteria;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import at.ac.univie.mminf.qskos4j.result.general.CollectionResult;
import at.ac.univie.mminf.qskos4j.util.vocab.SparqlPrefix;
import at.ac.univie.mminf.qskos4j.util.vocab.VocabRepository;

public class SkosTermsChecker extends Criterion {

	private Map<URI, Collection<URI>> deprecatedProperties, illegalTerms;
	
	public SkosTermsChecker(VocabRepository vocabRepository) {
		super(vocabRepository);
	}
	
	public CollectionResult<URI> findUndefinedSkosResources() throws OpenRDFException
	{
		findDeprecatedProperties();
		findIllegalTerms();
		
		return new CollectionResult<URI>(collectUndefinedResources());
	}
	
	private void findDeprecatedProperties() throws OpenRDFException 
	{
		TupleQueryResult result = vocabRepository.query(createDeprecatedPropertiesQuery());
		generateDeprecatedPropertiesMap(result);	
	}
	
	private String createDeprecatedPropertiesQuery() {
		return SparqlPrefix.SKOS +
			"SELECT DISTINCT ?iri ?deprProp "+
			"FROM <" +vocabRepository.getVocabContext()+ "> "+
			"WHERE {{?iri ?deprProp ?o .} UNION "+
				"{?q ?deprProp ?iri .} "+
				"FILTER isIRI(?iri) "+
				"FILTER (?deprProp IN (" +
					"skos:symbol,"+ 
					"skos:prefSymbol,"+
					"skos:altSymbol,"+
					"skos:CollectableProperty,"+
					"skos:subject,"+
					"skos:isSubjectOf,"+
					"skos:primarySubject,"+
					"skos:isPrimarySubjectOf,"+
					"skos:subjectIndicator))}";
	}
	
	private void generateDeprecatedPropertiesMap(TupleQueryResult result) 
		throws QueryEvaluationException 
	{
		deprecatedProperties = new HashMap<URI, Collection<URI>>();

		while (result.hasNext()) {
			BindingSet queryResult = result.next();
			URI resource = (URI) queryResult.getValue("iri");
			URI deprProperty = (URI) queryResult.getValue("deprProp");
			
			Collection<URI> resources = deprecatedProperties.get(deprProperty);
			if (resources == null) {
				resources = new HashSet<URI>();
				deprecatedProperties.put(deprProperty, resources);
			}
			resources.add(resource);
		}
	}
	
	private void findIllegalTerms() throws OpenRDFException 
	{
		TupleQueryResult result = vocabRepository.query(createIllegalTermsQuery());
		generateIllegalTermsMap(result);
	}
	
	private String createIllegalTermsQuery() {
		return SparqlPrefix.SKOS+ 
			"SELECT DISTINCT ?illTerm ?s ?o "+
			"FROM <" +vocabRepository.getVocabContext()+ "> "+
			"FROM NAMED <" +vocabRepository.SKOS_GRAPH_URL+ "> "+
			"WHERE {{?illTerm ?p ?o . } UNION "+
			"{?s ?illTerm ?o . } UNION "+
			"{?s ?p ?illTerm . } "+
			"FILTER isIRI(?illTerm) "+
			"FILTER STRSTARTS(str(?illTerm), \"" +SparqlPrefix.SKOS.getNameSpace()+ "\") "+
			"FILTER NOT EXISTS {"+
			"GRAPH <"+vocabRepository.SKOS_GRAPH_URL+"> {"+
			"?illTerm ?p1 ?o1}}} ";
	}
	
	private void generateIllegalTermsMap(TupleQueryResult result) 
		throws QueryEvaluationException 
	{
		illegalTerms = new HashMap<URI, Collection<URI>>();
		
		while (result.hasNext()) {
			BindingSet queryResult = result.next();
			URI illegalTerm = (URI) queryResult.getValue("illTerm");
			URI subject = (URI) queryResult.getValue("s");
			Value object = queryResult.getValue("o");

			if (!illegalTerm.getLocalName().isEmpty()) {
				addTermToMap(illegalTerm, subject, object);
			}
		}
	}
	
	private void addTermToMap(URI term, URI subject, Value object) {
		Collection<URI> resources = illegalTerms.get(term);
		if (resources == null) {
			resources = new HashSet<URI>();
			illegalTerms.put(term, resources);
		}
		
		if (subject != null) {
			resources.add(subject);
		}
		else if (object != null && object instanceof URI) {
			resources.add((URI) object);
		}		
	}
	
	private Collection<URI> collectUndefinedResources() {
		Collection<URI> undefRes = new HashSet<URI>();
		
		undefRes.addAll(deprecatedProperties.keySet());
		undefRes.addAll(illegalTerms.keySet());
		
		return undefRes;
	}
}
