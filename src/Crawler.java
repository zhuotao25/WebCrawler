import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Crawler {

	private int part=1; // 1 = PART A ; 2 = PART B
	private int numLinksToExtract = 100; 
	private Set<String> visitedLinks = new HashSet<String>(); // links visited
	private Set<String> uniqueLinks = new HashSet<String>(); // unique links
	private List<String> foundLinks = new LinkedList<String>(); // all links including duplicates
	private HashMap<String, ArrayList<String>> robotsMap = new HashMap<String, ArrayList<String>>(); // store robots.txt disallow for different hosts
	
	
	public Crawler(int partAB){ // 1 = PART A ; 2 = PART B
		part=partAB; 
		if(part==2){
			numLinksToExtract=1000; // change to 1000 when doing part B
		}
	}

	// start web crawler
	public void start(String url) throws InterruptedException {	
		//PrintWriter writer = new PrintWriter("final10002.txt", "UTF-8"); // for plotting graph

		while(uniqueLinks.size() < numLinksToExtract){ // if number of unique links less than max number of links to extract
			String tempUrl;

			if(foundLinks.size()==0){ // the beginning of crawling
				tempUrl = url;
			}
			else{ // get a new link to crawl
				tempUrl =foundLinks.remove(0);
				while(visitedLinks.contains(tempUrl)){
					tempUrl=foundLinks.remove(0);
				}
			}	
			visitedLinks.add(tempUrl); // store the visited link 
			uniqueLinks.add(tempUrl); 
			crawl(tempUrl); // start crawling
			Thread.sleep(5000); // after crawl a page, sleep for 5s
			//writer.println(visitedLinks.size()+","+uniqueLinks.size());// for plotting graph
			System.out.println("==Visited "+visitedLinks.size());
			System.out.println("==To Visit "+foundLinks.size());
			System.out.println("==Unique "+uniqueLinks.size());
		}
		for (String s : uniqueLinks) { // for debugging 
			System.out.println(s);
		}
		//writer.close(); // for plotting graph
	}

	// main crawl method
	@SuppressWarnings("finally")
	private void crawl(String url) {
		System.out.println(" trying >>> " + url);
		Boolean isAllowed= checkRobots(url); // check if the page is allowed to visit
		if(isAllowed){ // if it is allowed
			try{
				Connection connection = Jsoup.connect(url).timeout(5000); // set up connection
				Document html = connection.get(); // get html document
				Elements links = html.select("a[href]"); // get all "a" tags
				System.out.println("There are " + links.size() + " links on this page");
				for(Element link : links){ // for each link
					// if it is PART A AND link contains ".cs.umass.edu/" or ".cics.umass.edu/"
					if((part==1)&&(link.attr("abs:href").toLowerCase().contains(".cs.umass.edu/")||link.attr("abs:href").toLowerCase().contains(".cics.umass.edu/"))){
						String s=link.toString();
						// filter out bad links
						if(s.contains("tel:")||s.contains("mailto:")||s.contains("javascript:")||s.contains("http:/w")){
							System.out.println("bad link");
						}
						else{
							try{
								// get the contentType of the page
								Response res = Jsoup.connect(link.attr("abs:href")).execute();
								String contentType = res.contentType(); 
								// save only the links to html or pdf
								if(contentType.contains("text/html")||contentType.contains("application/pdf")){
									if(!link.attr("href").startsWith("#")){ // get rid of the link loop back to itself
										foundLinks.add(link.attr("abs:href"));
										uniqueLinks.add(link.attr("abs:href"));
									
										if(uniqueLinks.size()==numLinksToExtract){
											break;
										}
									}
								}
							} finally{ //if something goes wrong, just continue to check the next link
								//continue;
							}
						}
					}
					// #################################################################################################
					// ################################### PART B IS HERE ##############################################
					// #################################################################################################
					// If it is PART B, no filters
					else if(part==2){
						String s=link.toString();
						if(s.contains("tel:")||s.contains("mailto:")||s.contains("javascript:")||s.contains("http:/w")){
							System.out.println("bad links");
						}
						else{
							try{
								// get the contentType of the page
								Response res = Jsoup.connect(link.attr("abs:href")).timeout(5000).execute();
								String contentType = res.contentType(); 
								// save only the links to html or pdf
								if(contentType.contains("html")||contentType.contains("pdf")){
									if(!link.attr("href").startsWith("#")){ // get rid of the link loop back to itself
										foundLinks.add(link.attr("abs:href"));
										uniqueLinks.add(link.attr("abs:href"));
										if(uniqueLinks.size()==numLinksToExtract){
											break;
										}
									}
								}
							} finally{ //if something goes wrong, just continue to check the next link
								continue;
							}
						}
					}
				}

			}
			catch(IOException ioe){
				System.out.println(ioe);
			}
		}
		else{ // If page is not allowed to visit
			System.out.println("disallow!");
		}
	}

	// check if the url is allowed to visit 
	private Boolean checkRobots(String url) {
		try{
			URL curURL=new URL(url);
			String host=curURL.getHost(); // get the host of url
			ArrayList<String> disallowArr =robotsMap.get(host); // list of disallow for the certain host
			if (disallowArr==null){ // if we can't find the list of disallow for that host
				disallowArr=new ArrayList<String>(); // create a new list

				String robotLink="http://"+host+"/robots.txt";
				URL robotURL=new URL(robotLink); // url for robots.txt
				try {
					// try to connect to robots.txt and read
					BufferedReader in = new BufferedReader(new InputStreamReader(robotURL.openStream()));
					String s;
					while ((s = in.readLine()) != null) { // read robots.txt line by line
						String dis="Disallow: ";
						if (s.indexOf(dis)==0) { // find "Disallow: "
							String disallowPath=s.substring(dis.length()); // get the path after "Disallow"
							disallowArr.add(disallowPath); // add the path to disallow list
						}
					}
					robotsMap.put(host, disallowArr); // map the host with disallow list
				} catch (IOException e) {
					e.printStackTrace();
					return true; // return true when we can't find robots.txt
				}
			}
			// check if the url is allowed to visit by comparing with the path in disallow list
			String path=curURL.getPath(); // get the path of url
			for (int i=0;i<disallowArr.size(); i++) { // for each disallow path in disallow list
				if (path.indexOf(disallowArr.get(i))==0) { // if match 
					return false; 
				}
			}
			// otherwise
			return true;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return false;
		}
	}


	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, InterruptedException{	
		Crawler spider = new Crawler(1); // // 1 = PART A ; 2 = PART B
		spider.start("http://ciir.cs.umass.edu/");//http://ciir.cs.umass.edu/
	}
}
