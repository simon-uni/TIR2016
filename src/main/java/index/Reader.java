package index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Reader {
	/*
	public static void main(String[] args) {
		String dir = "/home/swingert/workspace/Index";
		String query = "football players in spain@ 2000-2013";
		try {
			Searcher.search(dir, query);
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		try {
			System.out.println("Type in your query: ");
		String dir = "/home/swingert/workspace/Index";
		Directory indexDir = FSDirectory.open(Paths.get(new File(dir).toURI()));
		
		IndexReader rdr = DirectoryReader.open(indexDir);
		IndexSearcher is = new IndexSearcher(rdr);
		QueryParser parser = new QueryParser("content", new StandardAnalyzer());
		
		Scanner sc = new Scanner (System.in);
		String s1 = "";
		while(sc.hasNext() && !s1.equals("exit")) {
			
			s1 = sc.nextLine();
			Query q;
			if (s1.lastIndexOf("@") != -1)
			{
				int endOfQuery = s1.lastIndexOf("@");
				String[] years = s1.substring(endOfQuery+1).trim().split("-");
				Query dateQuery = LongPoint.newRangeQuery("date", Long.parseLong(years[0]), Long.parseLong(years[1]));
				Query contentQuery = parser.parse(s1.substring(0, endOfQuery));
				BooleanQuery.Builder qb = new BooleanQuery.Builder();
				qb.add(dateQuery, Occur.MUST);
				qb.add(contentQuery, Occur.MUST);
				q = qb.build();
				
			} else {
				q = parser.parse(s1);
			}
			
			TotalHitCountCollector col = new TotalHitCountCollector();
			is.search(q, col);
//			TopDocs docs = is.search(q, 10);
			
			System.out.println("Total hits: " + col.getTotalHits());
			System.out.println("Type in your query: ");
//			if (docs.totalHits >= 1 && docs.scoreDocs[0] != null)
//			{
//				System.out.println(is.doc(docs.scoreDocs[0].doc).getField("title"));
//				System.out.println(is.doc(docs.scoreDocs[0].doc).getField("id"));
//			}
			
		}
		sc.close();
		rdr.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	*/
}
