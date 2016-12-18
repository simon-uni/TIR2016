package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.management.RuntimeErrorException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main1 {

	private static class QDRel {
		final int query, relevance;
		final String document;
		public QDRel(int query, int relevance, String document) {
			this.query = query;
			this.relevance = relevance;
			this.document = document;
		}
	}
	
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
			
			Map<Integer, Topic> topics = new HashMap<Integer, Topic>();
			Document doc = Jsoup.parse(new File(queryFile), "UTF-8", "");
			DateFormat df = new SimpleDateFormat("MMM dd, yyyy z", Locale.ENGLISH);
			
			for (Element e : doc.getElementsByTag("topic")) {
				String id = e.getElementsByTag("id").get(0).text();
				String title = e.getElementsByTag("title").get(0).text();
				String desc = e.getElementsByTag("description").get(0).text();
				long time = df.parse(e.getElementsByTag("query_issue_time").get(0).text()).getTime();
				String atemporal = e.getElementsByTag("subtopics").get(0).children().get(0).text();
				String past = e.getElementsByTag("subtopics").get(0).children().get(1).text();
				String recent = e.getElementsByTag("subtopics").get(0).children().get(2).text();
				String future = e.getElementsByTag("subtopics").get(0).children().get(3).text();
				Topic t = new Topic(id, title, desc, atemporal, future, recent, past, time);
				topics.put(Integer.parseInt(id), t);
			}
			
			Directory indexDir = FSDirectory.open(Paths.get(new File(dir).toURI()));
			IndexReader rdr = DirectoryReader.open(indexDir);
			IndexSearcher is = new IndexSearcher(rdr);
			
			List<QDRel> atemporalList = new ArrayList<QDRel>();
			List<QDRel> pastList = new ArrayList<QDRel>();
			List<QDRel> recentList = new ArrayList<QDRel>();
			List<QDRel> futureList = new ArrayList<QDRel>();
		    String line;
		    while ((line = br.readLine()) != null) {
		    	
		    	String qID = line.split(" ")[0];
		    	int query = Integer.parseInt(qID.substring(0, 2));
		    	String document = line.split(" ")[1];
		    	String rel = line.split(" ")[2];
		    	int relevance = Integer.parseInt(rel.substring(1, 1));
				
		    	switch (qID.substring(3, 3)) {
				case "a":
					atemporalList.add(new QDRel(query, relevance, document));
					break;
				case "p":
					pastList.add(new QDRel(query, relevance, document));
					break;
				case "r":
					recentList.add(new QDRel(query, relevance, document));
					break;
				case "f":
					futureList.add(new QDRel(query, relevance, document));
					break;
				default:
					throw new RuntimeException("Parsing went wrong ;(");
				}
		    	
		    }
		    
		    
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

	}

	
	private List<String> createFile(List<QDRel> qrels, IndexSearcher is, Map<Integer, Topic> topics) {
		List<String> lines = new ArrayList<String>();
		
		for (QDRel qrel : qrels) {
			StringBuilder outLine = new StringBuilder();
			
			outLine.append(qrel.relevance + " qid:" + qrel.query + " ");
			
			//First Feature 
			
			
			//Second Feature: Time Distance
			
			
			lines.add(outLine.toString());
		}
		
		return null;
	}
}
