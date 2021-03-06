package at.ac.univie.mminf.qskos4j.criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

import at.ac.univie.mminf.qskos4j.result.general.CollectionResult;
import at.ac.univie.mminf.qskos4j.util.vocab.SparqlPrefix;
import at.ac.univie.mminf.qskos4j.util.vocab.VocabRepository;

public class ConceptSchemeChecker extends Criterion {

	public ConceptSchemeChecker(VocabRepository vocabRepository) {
		super(vocabRepository);
	}

	public CollectionResult<URI> findOmittedTopConcepts(Collection<URI> allConceptSchemes) 
		throws OpenRDFException
	{
		RepositoryConnection connection = vocabRepository.getRepository().getConnection();
		Collection<URI> csWithOmittedTopConcepts = new HashSet<URI>();
		
		for (URI conceptScheme : allConceptSchemes) {
			BooleanQuery hasTopConceptQuery = connection.prepareBooleanQuery(
				QueryLanguage.SPARQL, 
				createConceptSchemeWithoutTopConceptQuery(conceptScheme));
			
			if (!hasTopConceptQuery.evaluate()) {
				csWithOmittedTopConcepts.add(conceptScheme);
			}
		}
		
		return new CollectionResult<URI>(csWithOmittedTopConcepts);
	}
	
	private String createConceptSchemeWithoutTopConceptQuery(URI conceptScheme) {
		return SparqlPrefix.SKOS+
			"ASK {" +
				"<"+conceptScheme.stringValue()+"> (skos:hasTopConcept|^skos:topConceptOf)+ ?topConcept"+
			"}";	
	}
	
	public CollectionResult<URI> findTopConceptsHavingBroaderConcepts() 
			throws OpenRDFException
		{
			TupleQueryResult result = vocabRepository.query(createTopConceptsHavingBroaderConceptQuery());
			Collection<URI> foundTopConcepts = createUriResultList(result, "topConcept");
			return new CollectionResult<URI>(foundTopConcepts);
		}

	
	private String createTopConceptsHavingBroaderConceptQuery() {
		return SparqlPrefix.SKOS + 
			"SELECT DISTINCT ?topConcept WHERE " +
			"{" +
				"{?topConcept skos:topConceptOf ?conceptScheme1}" +
				"UNION" +
				"{?conceptScheme2 skos:hasTopConcept ?topConcept}" +
				"?topConcept skos:broader|skos:broaderTransitive|^skos:narrower|^skos:narrowerTransitive ?broaderConcept ." +
			"}";
	}

	private Collection<URI> createUriResultList(
		TupleQueryResult result, 
		String bindingName) throws OpenRDFException
	{
		List<URI> resultList = new ArrayList<URI>();
		
		while (result.hasNext()) {
			BindingSet queryResult = result.next();
			URI resource = (URI) queryResult.getValue(bindingName);
			resultList.add(resource);
		}
		
		return resultList;
	}
	
}
