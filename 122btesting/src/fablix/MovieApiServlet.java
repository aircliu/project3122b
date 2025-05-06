package fablix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/api/movies")
public class MovieApiServlet extends HttpServlet {
    private static final String URL = "jdbc:mysql://localhost:3306/moviedb";
    private static final String USER = "mytestuser";
    private static final String PASS = "My6$Password";
    
    // Cache implementation
    private static class MoviesCache {
        private static final int MAX_CACHE_SIZE = 100;
        private static final Map<CacheKey, JsonObject> cache = new ConcurrentHashMap<>();
        private static final LinkedList<CacheKey> cacheOrder = new LinkedList<>();  // Track insertion order
        
        public static class CacheKey {
            private final String title;
            private final String year;
            private final String director;
            private final String star;
            private final String genre;
            private final String initial;
            private final String sort1;
            private final String dir1;
            private final String sort2;
            private final String dir2;
            private final int pageSize;
            private final int pageNumber;
            
            public CacheKey(String title, String year, String director, String star, 
                          String genre, String initial, String sort1, String dir1, 
                          String sort2, String dir2, int pageSize, int pageNumber) {
                this.title = title;
                this.year = year;
                this.director = director;
                this.star = star;
                this.genre = genre;
                this.initial = initial;
                this.sort1 = sort1;
                this.dir1 = dir1;
                this.sort2 = sort2;
                this.dir2 = dir2;
                this.pageSize = pageSize;
                this.pageNumber = pageNumber;
            }
            
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CacheKey cacheKey = (CacheKey) o;
                return pageSize == cacheKey.pageSize &&
                       pageNumber == cacheKey.pageNumber &&
                       Objects.equals(title, cacheKey.title) &&
                       Objects.equals(year, cacheKey.year) &&
                       Objects.equals(director, cacheKey.director) &&
                       Objects.equals(star, cacheKey.star) &&
                       Objects.equals(genre, cacheKey.genre) &&
                       Objects.equals(initial, cacheKey.initial) &&
                       Objects.equals(sort1, cacheKey.sort1) &&
                       Objects.equals(dir1, cacheKey.dir1) &&
                       Objects.equals(sort2, cacheKey.sort2) &&
                       Objects.equals(dir2, cacheKey.dir2);
            }
            
            @Override
            public int hashCode() {
                return Objects.hash(title, year, director, star, genre, initial, 
                                   sort1, dir1, sort2, dir2, pageSize, pageNumber);
            }
        }
        
        public static JsonObject get(CacheKey key) {
            return cache.get(key);
        }
        
        public static synchronized void put(CacheKey key, JsonObject value) {
            // If cache is full, remove oldest entry
            while (cache.size() >= MAX_CACHE_SIZE && !cacheOrder.isEmpty()) {
                CacheKey oldestKey = cacheOrder.removeFirst();
                cache.remove(oldestKey);
            }
            
            // Add new entry
            cache.put(key, value);
            cacheOrder.addLast(key);
            
            System.out.println("Cache size: " + cache.size() + " entries");
        }
    }
    
    /* ---------- helper to clamp page size ---------- */
    private int safeSize(String val) {
        try {
            int n = Integer.parseInt(val);
            switch (n) {
                case 10:
                case 25:
                case 50:
                case 100:
                    return n;
                default:
                    return 10;
            }
        } catch (Exception e) { return 10; }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        /* -------- read search / browse params -------- */
        String title    = req.getParameter("title");
        String yearStr  = req.getParameter("year");
        String director = req.getParameter("director");
        String star     = req.getParameter("star");
        String genre    = req.getParameter("genre");
        String initial  = req.getParameter("initial");
        
        /* -------- sorting & paging params ------------ */
        String sort1 = Optional.ofNullable(req.getParameter("sort1")).orElse("title"); // title|rating
        String dir1  = Optional.ofNullable(req.getParameter("dir1")).orElse("asc");   // asc|desc
        String sort2 = Optional.ofNullable(req.getParameter("sort2")).orElse("rating"); // rating|title
        String dir2  = Optional.ofNullable(req.getParameter("dir2")).orElse("asc");   // asc|desc
        int size  = safeSize(req.getParameter("size"));
        int page  = Math.max(1, Integer.parseInt(
                    Optional.ofNullable(req.getParameter("page")).orElse("1")));
        int offset = (page - 1) * size;
        
        /* -------- Create cache key and check cache -------- */
        MoviesCache.CacheKey cacheKey = new MoviesCache.CacheKey(
            title, yearStr, director, star, genre, initial,
            sort1, dir1, sort2, dir2, size, page
        );

        JsonObject cachedResult = MoviesCache.get(cacheKey);
        if (cachedResult != null) {
            System.out.println("Cache hit for movie list API request");
            // Return cached result
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();
            out.write(cachedResult.toString());
            return;
        }

        System.out.println("Cache miss for movie list API request");
        
        /* -------- build dynamic SQL ------------------ */
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating\n");
        sql.append("FROM movies m\n");
        sql.append("LEFT JOIN ratings r ON m.id = r.movieId\n");
        
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        if (star != null && !star.isEmpty()) {
            sql.append("JOIN stars_in_movies sim ON m.id = sim.movieId\n");
            sql.append("JOIN stars s ON sim.starId = s.id\n");
        }
        
        if (genre != null && !genre.isEmpty()) {
            sql.append("JOIN genres_in_movies gim ON m.id = gim.movieId\n");
            sql.append("JOIN genres g ON gim.genreId = g.id\n");
        }
        
        if (title != null && !title.isEmpty()) { 
            where.add("m.title LIKE ?"); 
            params.add('%' + title + '%'); 
        }
        
        if (yearStr != null && !yearStr.isEmpty()) {
            try {
                where.add("m.year = ?");
                params.add(Integer.parseInt(yearStr));
            } catch (NumberFormatException ignored) { /* bad year → ignore */ }
        }
        
        if (director != null && !director.isEmpty()) { 
            where.add("m.director LIKE ?"); 
            params.add('%' + director + '%'); 
        }
        
        if (star != null && !star.isEmpty()) { 
            where.add("s.name LIKE ?"); 
            params.add('%' + star + '%'); 
        }
        
        if (genre != null && !genre.isEmpty()) { 
            where.add("g.name = ?"); 
            params.add(genre); 
        }
        
        if (initial != null) {
            if ("*".equals(initial))
                where.add("m.title REGEXP '^[^0-9A-Za-z]'");
            else {
                where.add("UPPER(m.title) LIKE ?");
                params.add(initial.toUpperCase() + "%");
            }
        }
        
        // Build the WHERE clause for both count and data queries
        String whereClause = "";
        if (!where.isEmpty())
            whereClause = "WHERE " + String.join(" AND ", where) + " ";
            
        // Create COUNT query to determine total results
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(DISTINCT m.id) as total\n");
        countSql.append("FROM movies m\n");
        countSql.append("LEFT JOIN ratings r ON m.id = r.movieId\n");
        
        // Add the same JOINs to the count query
        if (star != null && !star.isEmpty()) {
            countSql.append("JOIN stars_in_movies sim ON m.id = sim.movieId\n");
            countSql.append("JOIN stars s ON sim.starId = s.id\n");
        }
        
        if (genre != null && !genre.isEmpty()) {
            countSql.append("JOIN genres_in_movies gim ON m.id = gim.movieId\n");
            countSql.append("JOIN genres g ON gim.genreId = g.id\n");
        }
        
        // Add WHERE clause to count query
        countSql.append(whereClause);
        
        // Complete the main query
        sql.append(whereClause);
        sql.append("ORDER BY ").append(sort1).append(' ').append(dir1)
           .append(',').append(' ').append(sort2).append(' ').append(dir2)
           .append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        
        /* -------- execute query & return JSON ------------- */
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                // Create response JSON object
                JsonObject responseJson = new JsonObject();
                
                // Get all genres for the browse section
                JsonArray genresArray = new JsonArray();
                try (PreparedStatement genrePs = con.prepareStatement("SELECT name FROM genres ORDER BY name ASC")) {
                    ResultSet genreRs = genrePs.executeQuery();
                    while (genreRs.next()) {
                        genresArray.add(genreRs.getString("name"));
                    }
                }
                responseJson.add("genres", genresArray);
                
                // Get sort options information
                JsonObject sortOptions = new JsonObject();
                sortOptions.addProperty("sort1", sort1);
                sortOptions.addProperty("dir1", dir1);
                sortOptions.addProperty("sort2", sort2);
                sortOptions.addProperty("dir2", dir2);
                sortOptions.addProperty("size", size);
                responseJson.add("sortOptions", sortOptions);
                
                // First, get total count for pagination
                int totalMovies = 0;
                try (PreparedStatement countPs = con.prepareStatement(countSql.toString())) {
                    for (int i = 0; i < params.size() - 2; i++) {
                        countPs.setObject(i+1, params.get(i));
                    }
                    ResultSet countRs = countPs.executeQuery();
                    if (countRs.next()) {
                        totalMovies = countRs.getInt("total");
                    }
                }
                
                // Calculate pagination information
                int totalPages = (int) Math.ceil((double) totalMovies / size);
                JsonObject paginationInfo = new JsonObject();
                paginationInfo.addProperty("currentPage", page);
                paginationInfo.addProperty("totalPages", totalPages);
                paginationInfo.addProperty("pageSize", size);
                paginationInfo.addProperty("totalMovies", totalMovies);
                responseJson.add("pagination", paginationInfo);
                
                // Now execute the main query to get movies
                JsonArray moviesArray = new JsonArray();
                try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) ps.setObject(i+1, params.get(i));
                    ResultSet rs = ps.executeQuery();
                    
                    while (rs.next()) {
                        String mid = rs.getString("id");
                        String mtitle = rs.getString("title");
                        int myear = rs.getInt("year");
                        String mdir = rs.getString("director");
                        String mrat = rs.getString("rating");
                        
                        JsonObject movieJson = new JsonObject();
                        movieJson.addProperty("id", mid);
                        movieJson.addProperty("title", mtitle);
                        movieJson.addProperty("year", myear);
                        movieJson.addProperty("director", mdir);
                        movieJson.addProperty("rating", mrat);
                        
                        // Get genres for this movie
                        JsonArray movieGenres = new JsonArray();
                        try (PreparedStatement gq = con.prepareStatement(
                                "SELECT g.name " +
                                "FROM genres_in_movies gim " +
                                "JOIN genres g ON gim.genreId = g.id " +
                                "WHERE gim.movieId = ? " +
                                "ORDER BY g.name " +
                                "LIMIT 3")) {
                            gq.setString(1, mid);
                            ResultSet grs = gq.executeQuery();
                            while (grs.next()) {
                                JsonObject genreJson = new JsonObject();
                                genreJson.addProperty("name", grs.getString("name"));
                                movieGenres.add(genreJson);
                            }
                        }
                        movieJson.add("genres", movieGenres);
                        
                        // Get stars for this movie
                        JsonArray movieStars = new JsonArray();
                        try (PreparedStatement sq = con.prepareStatement(
                                "SELECT s.id, s.name, (SELECT count(DISTINCT movieId) FROM stars_in_movies subSm WHERE subSm.starId = s.id) as cnt " +
                                "FROM stars_in_movies sim " +
                                "JOIN stars s ON sim.starId = s.id " +
                                "WHERE sim.movieId = ? " +
                                "ORDER BY cnt DESC, s.name ASC " +
                                "LIMIT 3")) {
                            sq.setString(1, mid);
                            ResultSet srs = sq.executeQuery();
                            while (srs.next()) {
                                JsonObject starJson = new JsonObject();
                                starJson.addProperty("id", srs.getString("id"));
                                starJson.addProperty("name", srs.getString("name"));
                                movieStars.add(starJson);
                            }
                        }
                        movieJson.add("stars", movieStars);
                        
                        moviesArray.add(movieJson);
                    }
                }
                responseJson.add("movies", moviesArray);
                
                // Save lastQuery in session
                HttpSession session = req.getSession();
                session.setAttribute("lastQuery", req.getQueryString() == null ? "" : req.getQueryString());
                
                // Only store in cache if not too large (total number of records <= 100)
                if (totalMovies <= 100) {
                    MoviesCache.put(cacheKey, responseJson);
                }
                
                // Return the JSON response
                out.write(responseJson.toString());
            }
        } catch (Exception e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", e.getMessage());
            out.write(errorJson.toString());
            e.printStackTrace();
        }
    }
}