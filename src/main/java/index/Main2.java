package index;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Scanner;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class Main2 {

	public enum Tag {
		Title, Date, Content, Nothing;
	}
	
	private static IndexWriter writer;
	
	public static void main(String[] args) {
		String dir = args[0];
		String data = args[1];
		String what = args[2];
		final float parameter = Float.parseFloat(args[3]);
		
		//----------------------------
		// Initialization
		//----------------------------
//		String dir = "/tmp";
//		String data = "Data";
		
		try {
			Directory indexDir = FSDirectory.open(Paths.get(new File(dir).toURI()));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
			cfg.setOpenMode(OpenMode.CREATE);
			writer = new IndexWriter(indexDir, cfg);
		
		//----------------------------
		// Read the data
		//----------------------------
		File d = new File(data);
		if (!data.equalsIgnoreCase("skip"))
		{
			for (File f : d.listFiles())
			{
				InputStream io;
				try {
					// Fix invalid XML
					System.out.println(f.getName());
					io = new FileInputStream(f);
					String text = IOUtils.toString(io, Charset.forName("ISO-8859-1"));
					text = text.replaceAll("&mdash;", "-");
					text = text.replaceAll("&deg;", "deg");
					text = text.replaceAll("(&)(?!\\S{1,10};)", "&amp;");
					
					io = new ByteArrayInputStream(text.getBytes(Charset.forName("ISO-8859-1")));
					
					XMLInputFactory inFactory = XMLInputFactory.newInstance();
					XMLEventReader eventReader = inFactory.createXMLEventReader(io);
					
					StringBuilder title = new StringBuilder(), date = new StringBuilder(),
							content = new StringBuilder(), id = new StringBuilder();
					Tag tag = Tag.Nothing;
					
					// Read the XML file
					while(eventReader.hasNext()){
						XMLEvent event = eventReader.nextEvent();
						if (event.isStartElement() &&
								event.asStartElement().getName().getLocalPart().equalsIgnoreCase("tag") &&
								event.asStartElement().getAttributeByName(new QName("name")).getValue().equalsIgnoreCase("title"))
						{
							tag = Tag.Title;
						} else if (event.isStartElement() &&
								event.asStartElement().getName().getLocalPart().equalsIgnoreCase("tag") &&
								event.asStartElement().getAttributeByName(new QName("name")).getValue().equalsIgnoreCase("date"))
						{
							tag = Tag.Date;
						} else if (event.isStartElement() &&
								event.asStartElement().getName().getLocalPart().equalsIgnoreCase("text")) {
							tag = Tag.Content;
						} else if (event.isStartElement() &&
								event.asStartElement().getName().getLocalPart().equalsIgnoreCase("doc"))
						{
							id.append(event.asStartElement().getAttributeByName(new QName("id")).getValue());
						} else if (event.isCharacters())
						{
							switch (tag) {
							case Title:
								title.append(event.asCharacters().getData());
								break;
							case Date:
								date.append(event.asCharacters().getData());
								break;
							case Content:
								content.append(event.asCharacters().getData());
								break;
							default:
								break;
							}
						} else if (event.isEndElement() &&
							event.asEndElement().getName().getLocalPart().equalsIgnoreCase("doc"))
						{		
//						System.out.println("New Doc\n" + title + "\n" + date + "\n" + content.substring(0, 20));
							if (title.toString().equals("") || id.toString().equals("") ||
									content.toString().equals("") || date.toString().equals(""))
							{
								System.err.println("Some tag is missing here");
							}
							addDoc(title.toString(), id.toString(), content.toString(),
//								Long.parseLong(date.toString().trim().substring(0, 4)));
									Integer.parseInt(date.toString().trim().substring(0, 4)));
							title = new StringBuilder();
							id = new StringBuilder();
							date = new StringBuilder();
							content = new StringBuilder();
						}
					}
	
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
			}
//			writer.commit();
			writer.close();
		}
		
		//----------------------------
		// Read in queries
		//----------------------------
		System.out.println("Type in your query: ");
		IndexReader rdr = DirectoryReader.open(indexDir);
		IndexSearcher is = new IndexSearcher(rdr);
		if (what.equalsIgnoreCase("jelinek"))
		{
			is.setSimilarity(new LMSimilarity() {
				@Override
				protected float score(BasicStats stats, float freq, float docLen) {
					return (float)Math.log((double)(1.0f + (1.0f-parameter) * (freq/docLen) +
							parameter * (freq/stats.getTotalTermFreq())));
				}
				@Override
				public String getName() {
					return "Jelinek-Mercer";
				}
			});
		} else {
			is.setSimilarity(new LMSimilarity() {
				@Override
				protected float score(BasicStats stats, float freq, float docLen) {
					return (float)Math.log((double)((freq+parameter*(freq/stats.getTotalTermFreq()))/
							(docLen+parameter)));
				}
				@Override
				public String getName() {
					return "Dirichlet";
				}
			});
		}
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
//				Query dateQuery = LongPoint.newRangeQuery("date", Long.parseLong(years[0]), Long.parseLong(years[1]));
				Query dateQuery = IntPoint.newRangeQuery("year", Integer.parseInt(years[0]), Integer.parseInt(years[1]));
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
			TopDocs docs = is.search(q, 10);
			
			System.out.println("Total hits: " + col.getTotalHits());
			
			int count = 0;
			for (ScoreDoc doc : docs.scoreDocs)
			{
				if (count >= 5)
				{
					break;
				} else {
					count++;
				}
				System.out.println(is.doc(doc.doc).getField("title").stringValue());
				System.out.println(is.doc(doc.doc).getField("id").stringValue());
			}
			
			System.out.println("Type in your query: ");
			
		}
		sc.close();
		rdr.close();
		
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void addDoc(String title, String id, String content, long date) throws IOException {
		  Document doc = new Document();
		  doc.add(new TextField("title", title, Store.YES));
		  doc.add(new TextField("id", id, Store.YES));
		  doc.add(new TextField("content", content, Store.YES));
//		  doc.add(new LongPoint("date", date));
		  doc.add(new IntPoint("year", (int)date));
		  writer.addDocument(doc);
	}


}
