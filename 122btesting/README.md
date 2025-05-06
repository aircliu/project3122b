# Fabflix Project

## Demo Videos

- Project 1: https://www.youtube.com/watch?v=kdpBQKqOAAw
- Project 2: https://www.youtube.com/watch?v=22zG4iko1q8

## Substring Matching Implementation

### How and Where We Use LIKE Predicate

In our implementation, we support substring matching for string fields using the SQL LIKE operator with wildcard characters. This allows users to search without providing exact values.

The implementation can be found in `MovieApiServlet.java` where we build our search queries:

```java
// For movie titles (string field)
if (title != null && !title.isEmpty()) {
    where.add("m.title LIKE ?");
    params.add('%' + title + '%');
}

// For directors (string field)
if (director != null && !director.isEmpty()) {
    where.add("m.director LIKE ?");
    params.add('%' + director + '%');
}

// For star names (string field)
if (star != null && !star.isEmpty()) {
    where.add("s.name LIKE ?");
    params.add('%' + star + '%');
}

// For year (numeric field) - exact matching only
if (yearStr != null && !yearStr.isEmpty()) {
    try {
        where.add("m.year = ?");
        params.add(Integer.parseInt(yearStr));
    } catch (NumberFormatException ignored) { /* bad year → ignore */ }
}
```

Architecture Changes (Project 2)
For Project 2, we separated the frontend and backend code:

Frontend:

HTML pages for UI structure
JavaScript for dynamic content rendering and AJAX calls
CSS for styling


Backend:

Java servlets that return JSON data
Database operations separated from presentation logic



Contributors
- Eric Liu
- Jiu Guo

we split the work 50/50
