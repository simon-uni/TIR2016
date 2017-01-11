package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BonusMain {

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
		
		String qNumber = args[0];
		String queryFile = args[1];
		String indexDir = args[2];
		String modelDir = args[3];
		String qrelFile = args[4];
		
		
		try (BufferedReader br = new BufferedReader(new FileReader(qrelFile))) {
			
			Map<Integer, Topic> topics = new HashMap<Integer, Topic>();
			Document doc = Jsoup.parse(new File(queryFile), "UTF-8", "");
			DateFormat df = new SimpleDateFormat("MMM dd, yyyy z", Locale.ENGLISH);
			
			System.out.println("Read topics");
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

			Directory indexDirec = FSDirectory.open(Paths.get(new File(indexDir).toURI()));
			IndexReader rdr = DirectoryReader.open(indexDirec);
			IndexSearcher is = new IndexSearcher(rdr);

			List<QDRel> atemporalList = new ArrayList<QDRel>();
			List<QDRel> pastList = new ArrayList<QDRel>();
			List<QDRel> recentList = new ArrayList<QDRel>();
			List<QDRel> futureList = new ArrayList<QDRel>();
			String line;
			System.out.println("Read QRels");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0) continue;
				
				String qID = line.split(" ")[0];
				int query = Integer.parseInt(qID.substring(0, 3));
				String document = line.split(" ")[1];
				String rel = line.split(" ")[2];
				int relevance = Integer.parseInt(rel.substring(1, 2));

				switch (qID.substring(3, 4)) {
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

			// ---------------------------------------------------------------
			// 1. Step: Query
			// ---------------------------------------------------------------
			System.out.println("1. Step: Query");
			QueryParser parser = new QueryParser("content", new StandardAnalyzer());
			try {
				Query q = parser.parse(topics.get(Integer.parseInt(qNumber)).title);
				// System.out.println(q.toString());
				TopDocs top = is.search(q, 1000);
				final List<String> lines = new ArrayList<String>();
				for (ScoreDoc d : top.scoreDocs) {
					String docText = is.doc(d.doc).get("content");
					// clean it
					List<String> sentences = new ArrayList<String>(Arrays.asList(docText.split("[.!?:]+")));
					sentences.removeIf(new Predicate<String>() {
						@Override
						public boolean test(String t) {
							return t.matches("^\\s*$");
						}
					});
					sentences.forEach(new Consumer<String>() {
						@Override
						public void accept(String t) {
							Pattern p = Pattern.compile("[a-zA-Z0-9]+");
							Matcher m = p.matcher(t);
							StringBuilder line = new StringBuilder();
							while (m.find()) {
							   line.append(m.group() + " ");
							}
							// System.out.println(line);
							if (line.length() > 0)
								lines.add(line.toString());
						}
					});
				}
				Files.write(Paths.get(modelDir + "/model.txt"), lines, Charset.forName("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE);
			
			// ---------------------------------------------------------------
			// 2. Step: Build the Word Embedding Model
			// ---------------------------------------------------------------
				System.out.println("2. Step: Build Word Embedding Model");
				String cmdarray = "" +
						"/home/swingert/workspace/WordEmbedding/bin/python " +
						"/home/swingert/workspace/WordEmbedding/deepLearner.py " +
						q.toString("content")
					;
				// System.out.println(q.toString("content"));
				Process p = Runtime.getRuntime().exec( cmdarray );

			    BufferedReader in = new BufferedReader( new InputStreamReader(p.getErrorStream()) );
			    while ((line = in.readLine()) != null) {
			    	System.err.println(line);
			    }
			    in = new BufferedReader( new InputStreamReader(p.getInputStream()) );
			    String expand = "";
			    while ((line = in.readLine()) != null) {
			    	System.out.println(line);
			    	expand = line;
			    }
			    in.close();
			
			// ---------------------------------------------------------------
			// 3. Step: Expand the query
			// ---------------------------------------------------------------
			    System.out.println("3. Step: Expand the query");
			    BooleanQuery.Builder boolQuery = new BooleanQuery.Builder();
			    boolQuery.add(q, Occur.SHOULD);
			    for (String word : expand.split(" ")) {
			    	boolQuery.add(parser.parse(word), Occur.SHOULD);
			    }
			    // System.out.println(boolQuery.build().toString());
			
			// ---------------------------------------------------------------
			// 4. Step: Compute Precision at 5, 10
			// ---------------------------------------------------------------
			    top = is.search(boolQuery.build(), 10);
			    
			
			} catch (NumberFormatException e1) {
				e1.printStackTrace();
			} catch (org.apache.lucene.queryparser.classic.ParseException e1) {
				e1.printStackTrace();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
	}
	
	private static List<String> createFile(List<QDRel> qrels, IndexSearcher is, Map<Integer, Topic> topics) {
		List<String> lines = new ArrayList<String>();
		QueryParser parser = new QueryParser("content", new StandardAnalyzer());

		try {
			for (QDRel qrel : qrels) {
//				System.out.println(qrel.document);
				StringBuilder outLine = new StringBuilder();

				outLine.append(qrel.relevance + " qid:" + qrel.query + " ");

				// First Feature
				TermQuery idQ = new TermQuery(new Term("id", qrel.document.trim()));
				TopDocs top = is.search(idQ, 2);
				if (top.totalHits != 1) {
//					System.out.println(top.totalHits);
//					throw new RuntimeException("Not the desired document could be fetched");
					System.err.println(qrel.document + " could not be fetched");
					continue;
				}
				Query q = parser.parse(topics.get(qrel.query).title);
				outLine.append("1:" + is.explain(q, top.scoreDocs[0].doc).getValue() + " ");

				// Second Feature: Time Distance
				outLine.append("2:" + (Long.parseLong(is.doc(top.scoreDocs[0].doc).get("date")) -
						topics.get(qrel.query).time));

				lines.add(outLine.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			e.printStackTrace();
		}

		return lines;
	}
}
