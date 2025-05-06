// Parse URL parameters
function getUrlParams() {
    const params = {};
    const queryString = window.location.search.substring(1);
    const pairs = queryString.split('&');
    
    for (let i = 0; i < pairs.length; i++) {
        const pair = pairs[i].split('=');
        if (pair[0] !== "") {
            params[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1] || "");
        }
    }
    
    return params;
}

// Build URL with parameters
function buildUrl(baseUrl, params) {
    // Start with the base URL
    let url = baseUrl;
    
    // Add question mark if we have parameters
    const paramKeys = Object.keys(params).filter(key => 
        params[key] !== undefined && params[key] !== "");
    
    if (paramKeys.length > 0) {
        url += "?";
    }
    
    // Add each parameter manually with proper encoding
    paramKeys.forEach((key, index) => {
        // Encode both key and value
        const encodedKey = encodeURIComponent(key);
        const encodedValue = encodeURIComponent(params[key]);
        
        url += encodedKey + "=" + encodedValue;
        
        // Add & if not the last parameter
        if (index < paramKeys.length - 1) {
            url += "&";
        }
    });
    
    return url;
}

// Handle the movie list data
function handleMovieListResult(resultData) {
    // Clear previous content
    $("#search-browse-container").empty();
    $("#controls-container").empty();
    $("#results-container").empty();
    $("#pagination-container").empty();
    
    const urlParams = getUrlParams();
    
    // Populate search and browse container
    populateSearchBrowseContainer(resultData.genres, urlParams);
    
    // Populate controls container
    populateControlsContainer(urlParams, resultData.sortOptions);
    
    // Populate results
    populateResultsContainer(resultData.movies);
    
    // Populate pagination
    populatePaginationContainer(resultData.pagination, urlParams);
}

// Populate the search and browse area
function populateSearchBrowseContainer(genres, urlParams) {
    let searchBrowseHtml = `
        <div style='display:flex; gap:20px; margin-bottom:20px;'>
            <!-- Search Form -->
            <div style='flex:1; padding:10px; background:#f8f8f8; border:1px solid #ddd;'>
                <h3>Search Movies</h3>
                <form id="search-form">
                    <div style='margin-bottom:8px;'>
                        <label style='display:inline-block; width:80px;'>Title:</label>
                        <input type='text' name='title' value='${urlParams.title || ""}'>
                    </div>
                    <div style='margin-bottom:8px;'>
                        <label style='display:inline-block; width:80px;'>Star:</label>
                        <input type='text' name='star' value='${urlParams.star || ""}'>
                    </div>
                    <div style='margin-bottom:8px;'>
                        <label style='display:inline-block; width:80px;'>Director:</label>
                        <input type='text' name='director' value='${urlParams.director || ""}'>
                    </div>
                    <div style='margin-bottom:8px;'>
                        <label style='display:inline-block; width:80px;'>Year:</label>
                        <input type='text' name='year' value='${urlParams.year || ""}'>
                    </div>
                    <div>
                        <button type='submit'>Search</button>
                        <button type='button' id="clear-search">Clear</button>
                    </div>
                </form>
            </div>
            
            <!-- Genre Browse -->
            <div style='flex:1; padding:10px; background:#f8f8f8; border:1px solid #ddd;'>
                <h3>Search by Genres</h3>
                <div style='display:flex; flex-wrap:wrap; max-height:180px; overflow-y:auto;'>`;
    
    // Add genres
    genres.forEach(genre => {
        searchBrowseHtml += `
            <div style='margin:5px 10px;'>
                <a href='movie-list?genre=${encodeURIComponent(genre)}' 
                   style='text-decoration:none; color:#333; background:#e9e9e9; padding:5px 10px; border-radius:3px;'>
                    ${genre}
                </a>
            </div>`;
    });
    
    searchBrowseHtml += `
                </div>
            </div>
        </div>
        
        <!-- Browse by genre links -->
        <div style='margin: 20px 0; text-align: center;'>
            <h2 style='color: #333;'>Browsing by movie genres</h2>
            <div style='display: flex; justify-content: center;'>
                <div style='display: flex; flex-wrap: wrap; justify-content: center;'>`;
    
    // Divide genres into 4 columns
    const genresPerColumn = Math.ceil(genres.length / 4);
    for (let i = 0; i < 4; i++) {
        searchBrowseHtml += `<div style='margin: 0 15px; text-align: center;'>`;
        
        for (let j = i * genresPerColumn; j < Math.min((i + 1) * genresPerColumn, genres.length); j++) {
            searchBrowseHtml += `
                <div style='margin: 8px 0;'>
                    <a href='movie-list?genre=${encodeURIComponent(genres[j])}' text-decoration: none;'>${genres[j]}</a>
                </div>`;
        }
        
        searchBrowseHtml += `</div>`;
    }
    
    searchBrowseHtml += `
                </div>
            </div>
        </div>
        
        <!-- Browse by title links -->
        <div style='margin: 30px 0; text-align: center;'>
            <h2 style='color: #333;'>Browsing by movie title</h2>
            <div style='display: flex; justify-content: center;'>
                <div style='margin: 10px 0;'>`;
    
    // A-Z links
    for (let ch = 'A'.charCodeAt(0); ch <= 'Z'.charCodeAt(0); ch++) {
        const letter = String.fromCharCode(ch);
        searchBrowseHtml += `
            <a href='movie-list?initial=${letter}' 
               style='display: inline-block; margin: 0 5px; text-decoration: none;'>${letter}</a>`;
    }
    
    searchBrowseHtml += `
                </div>
            </div>
            <div style='display: flex; justify-content: center;'>
                <div style='margin: 10px 0;'>`;
    
    // 0-9 links
    for (let n = 0; n <= 9; n++) {
        searchBrowseHtml += `
            <a href='movie-list?initial=${n}' 
               style='display: inline-block; margin: 0 5px; text-decoration: none;'>${n}</a>`;
    }
    
    // * link for non-alphanumerics
    searchBrowseHtml += `
            <a href='movie-list?initial=*' 
               style='display: inline-block; margin: 0 5px; text-decoration: none;'>*</a>
                </div>
            </div>
        </div>`;
    
    $("#search-browse-container").html(searchBrowseHtml);
    
    // Add event handlers
    $("#search-form").submit(function(event) {
        event.preventDefault();
        let formData = $(this).serializeArray();
        let searchParams = {};
        
        formData.forEach(item => {
            if (item.value.trim() !== "") {
                searchParams[item.name] = item.value;
            }
        });
        
        window.location.href = buildUrl("movie-list", searchParams);
    });
    
    $("#clear-search").click(function() {
        window.location.href = "movie-list";
    });
}

// Populate the controls container (sorting & page size)
function populateControlsContainer(urlParams, sortOptions) {
    const sort1 = urlParams.sort1 || "title";
    const dir1 = urlParams.dir1 || "asc";
    const sort2 = urlParams.sort2 || "rating";
    const dir2 = urlParams.dir2 || "asc";
    const size = urlParams.size || "10";
    
    let controlsHtml = `
        <form id="controls-form" style='margin-bottom:10px'>`;
    
    // Hidden fields for existing query params
    Object.keys(urlParams).forEach(key => {
        if (key !== 'sort1' && key !== 'dir1' && key !== 'sort2' && key !== 'dir2' && key !== 'size' && key !== 'page') {
            controlsHtml += `<input type='hidden' name='${key}' value='${urlParams[key]}'>`;
        }
    });
    
    controlsHtml += `
            Sort by:
            <select name='sort1'>
                <option value='title' ${sort1 === 'title' ? 'selected' : ''}>title</option>
                <option value='rating' ${sort1 === 'rating' ? 'selected' : ''}>rating</option>
            </select>
            <select name='dir1'>
                <option value='asc' ${dir1 === 'asc' ? 'selected' : ''}>asc</option>
                <option value='desc' ${dir1 === 'desc' ? 'selected' : ''}>desc</option>
            </select>
            <select name='sort2'>
                <option value='rating' ${sort2 === 'rating' ? 'selected' : ''}>rating</option>
                <option value='title' ${sort2 === 'title' ? 'selected' : ''}>title</option>
            </select>
            <select name='dir2'>
                <option value='asc' ${dir2 === 'asc' ? 'selected' : ''}>asc</option>
                <option value='desc' ${dir2 === 'desc' ? 'selected' : ''}>desc</option>
            </select>
            Page size:
            <select name='size'>
                <option value='10' ${size === '10' ? 'selected' : ''}>10</option>
                <option value='25' ${size === '25' ? 'selected' : ''}>25</option>
                <option value='50' ${size === '50' ? 'selected' : ''}>50</option>
                <option value='100' ${size === '100' ? 'selected' : ''}>100</option>
            </select>
            <button type='submit'>Apply</button>
        </form>`;
    
    $("#controls-container").html(controlsHtml);
    
    // Add event handler
    $("#controls-form").submit(function(event) {
        event.preventDefault();
        let formData = $(this).serializeArray();
        let params = {};
        
        formData.forEach(item => {
            params[item.name] = item.value;
        });
        
        window.location.href = buildUrl("movie-list", params);
    });
}

// Populate the results container
function populateResultsContainer(movies) {
    if (!movies || movies.length === 0) {
        $("#results-container").html("<p>No movies found.</p>");
        return;
    }
    
    let resultsHtml = `
        <table>
            <tr>
                <th>Title</th>
                <th>Year</th>
                <th>Director</th>
                <th>Genres</th>
                <th>Stars</th>
                <th>Rating</th>
                <th></th>
            </tr>`;
    
    movies.forEach(movie => {
        // Format genres
        let genresHtml = "";
        movie.genres.forEach((genre, index) => {
            if (index > 0) genresHtml += ", ";
            genresHtml += `<a href='movie-list?genre=${encodeURIComponent(genre.name)}'>${genre.name}</a>`;
        });
        
        // Format stars
        let starsHtml = "";
        movie.stars.forEach((star, index) => {
            if (index > 0) starsHtml += ", ";
            starsHtml += `<a href='single-star.html?starId=${star.id}'>${star.name}</a>`;
        });
        
        resultsHtml += `
            <tr>
                <td><a href='single-movie.html?movieId=${movie.id}'>${movie.title}</a></td>
                <td>${movie.year}</td>
                <td>${movie.director}</td>
                <td>${genresHtml}</td>
                <td>${starsHtml}</td>
                <td>${movie.rating || "N/A"}</td>
                <td>
                    <form class='add-to-cart-form' method='post' style='display:inline'>
                        <input type='hidden' name='movieId' value='${movie.id}'>
                        <button type='submit'>Add&nbsp;to&nbsp;Cart</button>
                    </form>
                </td>
            </tr>`;
    });
    
    resultsHtml += `</table>`;
    
    $("#results-container").html(resultsHtml);
    attachAddToCartHandler();
}

// Populate the pagination container
function populatePaginationContainer(pagination, urlParams) {
    const currentPage = parseInt(urlParams.page) || 1;
    const isFirstPage = currentPage === 1;
    // Calculate total pages, ensuring at least 1 page is displayed even with 0 results
    const totalPages = Math.max(1, pagination.totalPages);
    const isLastPage = currentPage >= totalPages;
    
    let paginationHtml = `
        <div style='margin-top:8px'>`;
    
    // Instead of a form, use direct links with the correct page values
    let prevParams = {...urlParams, page: currentPage - 1};
    let nextParams = {...urlParams, page: currentPage + 1};
    
    paginationHtml += `
            <button id="prev-btn" ${isFirstPage ? 'disabled' : ''}>Prev</button>
            <span style="margin: 0 10px;">Page ${currentPage} of ${totalPages}</span>
            <button id="next-btn" ${isLastPage ? 'disabled' : ''}>Next</button>
        </div>`;
    
    $("#pagination-container").html(paginationHtml);
    
    // Add event handlers for the buttons
    $("#prev-btn").click(function() {
        if (!isFirstPage) {
            window.location.href = buildUrl("movie-list", prevParams);
        }
    });
    
    $("#next-btn").click(function() {
        if (!isLastPage) {
            window.location.href = buildUrl("movie-list", nextParams);
        }
    });
}

// Main function to load movie list data
function loadMovieList() {
    const urlParams = getUrlParams();
    
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/movies" + window.location.search,
        success: (resultData) => handleMovieListResult(resultData)
    });
}

// Initialize the page
$(document).ready(function() {
    loadMovieList();
});