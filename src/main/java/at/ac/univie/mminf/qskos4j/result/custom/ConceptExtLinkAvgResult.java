package at.ac.univie.mminf.qskos4j.result.custom;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openrdf.model.URI;

import at.ac.univie.mminf.qskos4j.result.Result;

public class ConceptExtLinkAvgResult extends Result<Map<URI, List<URL>>> {

	public ConceptExtLinkAvgResult(Map<URI, List<URL>> data) {
		super(data);
	}

	@Override
	public String getShortReport() {
		List<URL> allExtUrls = new ArrayList<URL>();
		for (List<URL> extUrls : getData().values()) {
			allExtUrls.addAll(extUrls);
		}
		
		float extLinkAvg = (float) allExtUrls.size() / (float) getData().keySet().size();
		return "value: " +extLinkAvg;
	}

	@Override
	public String getExtensiveReport() {
		String extensiveReport = "";
		
		for (URI concept : getData().keySet()) {
			List<URL> extResources = getData().get(concept);
			
			if (!extResources.isEmpty()) {
				extensiveReport += "concept: '" +concept.stringValue()+ "', external resources: " +extResources.toString()+ "\n";
			}
		}

		return extensiveReport;
	}

}