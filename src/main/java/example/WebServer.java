package example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.timeseries.AggregationType;
import redis.clients.jedis.timeseries.TSElement;
import redis.clients.jedis.timeseries.TSRangeParams;

public class WebServer extends NanoHTTPD {
	UnifiedJedis jedis;
	
    public WebServer() throws IOException {
        super(8080);
        
        jedis = new UnifiedJedis();
        
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("WebServer running at port " + this.getListeningPort());
    }

    @Override
    public Response serve(IHTTPSession session) {
    	System.out.println("URI: " + session.getUri());
    	
    	InputStream is = null;
   	
		try {
	    	String uri = session.getUri();
	    	if(uri.compareToIgnoreCase("/indexnames") == 0) {
	    		// -------------------------------- handle index names select list -----------------------------------
	    		List<String> names = jedis.zrange("index:names", 0, -1); // NOTE: use LIMIT 100 when list grows much larger
	    		ListIterator<String> iter = names.listIterator();
	    		
	    		JSONArray array = new JSONArray();
	    		while(iter.hasNext()) {
	    			String name = iter.next();
	    			JSONObject option = new JSONObject();
	    			option.put("id", name);
	    			option.put("name", name);
	    			array.put(option);
	    		}
	    		
	    		is = new ByteArrayInputStream(array.toString().getBytes());

	    	}
	    	else if(uri.startsWith("/data/")) {
	    		// ------------------------------------- handle chart data inquiry -------------------------------------
	    		String[] split = uri.split("/");
	    		String indexName = split[split.length - 2];
	    		String keyName = indexName + ":Last";
	    		long startTime = 0;
	    		long endTime = System.currentTimeMillis();
	    		long bucketDuration = Long.valueOf(split[split.length - 1]);
	    				
	    		TSRangeParams params = new TSRangeParams(startTime, endTime);
	    		params.align(startTime);

	    		params.aggregation(AggregationType.MAX, bucketDuration);
	    		List<TSElement> listHigh = jedis.tsRange(keyName, params);

	    		params.aggregation(AggregationType.MIN, bucketDuration);
	    		List<TSElement> listLow = jedis.tsRange(keyName, params);

	    		params.aggregation(AggregationType.FIRST, bucketDuration);
	    		List<TSElement> listOpen = jedis.tsRange(keyName, params);

	    		params.aggregation(AggregationType.LAST, bucketDuration);
	    		List<TSElement> listClose = jedis.tsRange(keyName, params);
	    		
    			ListIterator<TSElement> liOpen = listOpen.listIterator();
    			ListIterator<TSElement> liHigh = listHigh.listIterator();
    			ListIterator<TSElement> liLow = listLow.listIterator();
    			ListIterator<TSElement> liClose = listClose.listIterator();
    			
    			JSONArray array = new JSONArray();

    			// TODO: each element needs to have same elements length!
    			while(liOpen.hasNext()) {
    				TSElement elOpen = liOpen.next();
    				TSElement elHigh = liHigh.next();
    				TSElement elLow = liLow.next();
    				TSElement elClose = liClose.next();
    				
    				JSONObject point = new JSONObject();
    				point.put("x", elOpen.getTimestamp());
    				
    				JSONArray pointValues = new JSONArray();
    				pointValues.put(elOpen.getValue());
    				pointValues.put(elHigh.getValue());
    				pointValues.put(elLow.getValue());
    				pointValues.put(elClose.getValue());
    				
    				point.put("y", pointValues);
    				
    				array.put(point);
    			}

    			is = new ByteArrayInputStream(array.toString().getBytes());
	    	}
	    	else {
	    		// ---------------------------------------- handle index page ----------------------------------------
	        	is = getClass().getClassLoader().getResourceAsStream("index.html");
	    	}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return newChunkedResponse(Status.OK, "text/html", is);
    }
}