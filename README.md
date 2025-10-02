# Movie Finder

Movie Finder is a lightweight web application for searching movies and viewing concise details.  

## Frontend
Plain **HTML/CSS/JavaScript** served from `/static`.  

## Backend
**Java 17 + Spring Boot**.  

External data is fetched from **TMDB** strictly on the server side (the key is not exposed to the client; since this is a university assignment, as an exception the API key will be added to the repository).  

## Requirements
- JDK 17+  
- Internet access  
- TMDB API key  

## Configuration
Set the following in `src/main/resources/application.properties`:  
- `movie.api.base-url`  
- `movie.api.image-base-url`  
- `movie.api.key`  
- `movie.api.timeout-ms`  

## Run
- In IntelliJ IDEA start `MovieFinderApplication` (green triangle), or  
- Run `./gradlew bootRun`  

Then open [http://localhost:8080/](http://localhost:8080/).  

## Use
- Type a title and press **Search** to see a grid of cards  
- Click a card to open a **details modal**  

## Optional Favorites
- Stored **per browser session** in server memory (no accounts/DB)  
- They persist while the session cookie remains and the server is running  

## Manual Checks
- `GET /api/movies?query=…&page=…` — search  
- `GET /api/movies/{id}` — details  
- *(optional)*  
  - `GET /api/favorites`  
  - `POST /api/favorites` with JSON `{id,title,year,posterUrl}`  
  - `DELETE /api/favorites/{id}`  

---

This product uses the **TMDB API** but is **not endorsed or certified** by TMDB.  
➡️ Include proper TMDB attribution in the UI.
