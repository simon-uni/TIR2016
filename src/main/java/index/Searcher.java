package index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.store.FSDirectory;

/* searching a lucene-filedirectory-index. */
public class Searcher {
	
	public static void search(String indexDir, String q) throws IOException, ParseException {
		/**/System.out.println("########################################################");
		/**/System.out.println("Searching...");
		
		IndexReader rdr = DirectoryReader.open(FSDirectory.open(new File(indexDir).toPath()));
		
		/**/System.out.println(" in index with " + rdr.numDocs() + " documents.");
		
		IndexSearcher is = new IndexSearcher(rdr);
		
		/* working on the query. partitioning into keywords and the two years. */
		String[] queryParts = q.split("@");
		
		String keywords = queryParts[0].trim();
		String interval = queryParts[1].trim();

		String[] intervalValues = interval.split("-");
		String startStr = intervalValues[0].trim();
		String endStr = intervalValues[1].trim();
		
		int start = Integer.parseInt(startStr);
		int end = Integer.parseInt(endStr);
		
		/* building the date-range-query. it is inclusive. */
		Query dateRangeQuery = IntPoint.newRangeQuery("year", start, end);
		
		/* building the keywords-query. using the same StandardAnalyzer. */
		QueryParser contentQueryParser = new QueryParser("content", new StandardAnalyzer());
		Query contentQuery = contentQueryParser.parse(keywords);

		/* combining the two query parts. */
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		bqb.add(dateRangeQuery, BooleanClause.Occur.MUST);
		bqb.add(contentQuery, BooleanClause.Occur.MUST);
		BooleanQuery query = bqb.build();
		
//		/**/System.out.println(" ContentPart: " + keywords);
//		/**/System.out.println(" YearRangePart: " + start + " " + end);
		/**/System.out.println(" Query: " + query);
		
		TopDocs hits = is.search(query, 3);
		
		/**/System.out.println(" totalHits: " + hits.totalHits);
		
//		System.out.println(" top 3 Hits:");
//		int i = 1;
//		for( ScoreDoc scoreDoc : hits.scoreDocs ) {
//			Document doc = is.doc(scoreDoc.doc);
//			/**/System.out.println("  " + i + ":");
//			/**/System.out.println("   score: " + scoreDoc.score);
//			/**/System.out.println("   id:    " + doc.get("id"));
//			/**/System.out.println("   title: " + doc.get("title"));
//			/**/System.out.println("   year:  " + doc.get("yearStr"));
//			i++;
//		}
		/**/System.out.println("########################################################");
		/**/System.out.println("DONE");
		/**/System.out.println("########################################################");

//		is.close();
		rdr.close();
	}
}
