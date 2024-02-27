package example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONTokener;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.timeseries.DuplicatePolicy;
import redis.clients.jedis.timeseries.TSCreateParams;

public class TickerPoller extends Thread {
	UnifiedJedis jedis;
	
	public TickerPoller() {
		super();
		jedis = new UnifiedJedis();
		start();
	}
	
	public void run() {
		try {
			URL url = new URL("https://indodax.com/api/tickers");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
		    con.setUseCaches(false);
		    con.setDoOutput(false);
			
		    // send HTTP request
		    int responseCode = con.getResponseCode();
		    
		    if(responseCode == HttpURLConnection.HTTP_OK) {
			    // parse response  
			    BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
			    StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
			    String line;
			    while ((line = rd.readLine()) != null) {
			      response.append(line);
			      response.append('\n');
			    }
			    rd.close();
			    
			    JSONTokener tokener = new JSONTokener(response.toString());
			    JSONObject finalResult = new JSONObject(tokener);
			    
			    // parse JSON response ticker data
			    JSONObject tickers = (JSONObject) finalResult.get("tickers");
			    
		        TSCreateParams params = new TSCreateParams();
		        params.duplicatePolicy(DuplicatePolicy.FIRST);
			    
			    tickers.keySet().forEach(key -> {
			    	JSONObject value = (JSONObject) tickers.get(key);
			    	
//			    	System.out.println("Parsing: " + value);
			        
			    	// only process xxx_idr entries
			    	if(key.endsWith("_idr")) {
				        try {
				        	// store the index name, with same score (0 in this example) they will be sorted lexicographically
				        	jedis.zadd("index:names", 0, key);

				        	// store the current index value in Redis Timeseries
					        jedis.tsAdd(key + ":Last",  value.getLong("server_time") * 1000, value.getDouble("last"), params);
				        }
				        catch(Exception ex) {
				        	System.out.println(ex.getMessage());
				        }
			    	}				        
			    });
		    }
		    else {
		    	System.out.println("HTTP Response error! " + responseCode);
		    }
		    
		    con.disconnect();
			Thread.sleep(5000);
			run();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			jedis.close();
		}
	}
}
