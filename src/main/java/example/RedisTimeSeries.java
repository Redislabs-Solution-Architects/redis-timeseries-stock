package example;

public class RedisTimeSeries {
	public static void main(String[] args) throws Exception {
		// start data poller
		System.out.println("Starting data poller...");
		new TickerPoller();
		
		// start web server
		System.out.println("Starting web server...");
		new WebServer();
	}
}
