1. Run local Redis Stack server, or use Redis docker image with:
docker run -d --name redis-stack-server -p 6379:6379 redis/redis-stack-server:latest

2. Run the app with "mvn test"

3. Let running for 5-10 minutes so the app collects enough ticker data (5-10 mins) to be stored in Redis

4. Open localhost:8080

5. Pick index and experiment candlestick results with different aggregation durations