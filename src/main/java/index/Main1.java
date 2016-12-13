package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main1 {

	private static class Topic {
		final String id, title, description, atemporal, future, recent, past;
		final long time;
		public Topic(String id, String title, String description,
				String atemporal, String future, String recent, String past,
				long time) {
			this.id = id;
			this.title = title;
			this.description = description;
			this.atemporal = atemporal;
			this.future = future;
			this.recent = recent;
			this.past = past;
			this.time = time;
		}
	}
	
	public static void main(String[] args) {
		
		String qrelFile = args[0];
		String dir = args[1];
		String queryFile = args[2];
		
		
		try (BufferedReader br = new BufferedReader(new FileReader(qrelFile))) {
			
			
			Document doc = Jsoup.parse(new File(queryFile), "UTF-8", "");
			
			for (Element e : doc.getElementsByTag("topic")) {
				String id = e.getElementsByTag("id").get(0).text();
				String title = e.getElementsByTag("title").get(0).text();
				String desc = e.getElementsByTag("description").get(0).text();
				long time = e.getElementsByTag("query_issue_time").get(0).text();
				
			}
			
			Directory indexDir = FSDirectory.open(Paths.get(new File(dir).toURI()));
			IndexReader rdr = DirectoryReader.open(indexDir);
			IndexSearcher is = new IndexSearcher(rdr);
			
			StringBuilder out = new StringBuilder();
		    String line;
		    while ((line = br.readLine()) != null) {
		       
		    	String qID = line.split(" ")[0];
		    	String docID = line.split(" ")[1];
		    	String relevance = line.split(" ")[2];
				
		    	
		    	
		    	
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
