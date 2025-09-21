Movie Finder is a lightweight web application for searching movies and viewing concise details. 
Frontend: plain HTML/CSS/JavaScript served from /static. 
Backend: Java 17 + Spring Boot. 
External data is fetched from TMDB strictly on the server side (the key is not exposed to the client; since this is a university assignment, as an exception the API key will be added to the repository). 
Requirements: JDK 17+, internet access, TMDB API key. 
Configuration: set movie.api.base-url, movie.api.image-base-url, movie.api.key, and movie.api.timeout-ms in src/main/resources/application.properties. 
Run: in IntelliJ IDEA start MovieFinderApplication (green triangle) or run ./gradlew bootRun, then open http://localhost:8080/. 
Use: type a title and press Search to see a grid of cards; click a card to open a details modal. 
Optional Favorites are stored per browser session in server memory (no accounts/DB); they persist while the session cookie remains and the server is running. 
Manual checks: GET /api/movies?query=…\&page=… for search, GET /api/movies/{id} for details, and (optional) GET /api/favorites, POST /api/favorites with JSON {id,title,year,posterUrl}, DELETE /api/favorites/{id}. 

This product uses the TMDB API but is not endorsed or certified by TMDB; include proper TMDB attribution in the UI.
