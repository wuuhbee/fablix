/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */


/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */

function handleResult(resultData) {

    console.log("handleResult: populating movie info from resultData");

    // populate the movie info h3
    // find the empty h3 body by id "movie_info"
    let movieInfoElement = jQuery("#movie_info");
    let movie = resultData[0];
    let genreHtml = "";
    for (let i = 0; i < movie["genres"].length; i++){
        let genre = movie["genres"][i];
        genreHtml += `<a href="movie_list.html?id=${genre["genre_id"]}">${genre["genre_name"]}</a>`;
        if (i < movie["genres"].length - 1){
            genreHtml += ", ";
        }
    }
    // append html <p> created to the h3 body, which will refresh the page
    movieInfoElement.append("<h1>" + movie["movie_title"] + "</h1>" +
        "<p class='sub-info'>Release Year: " + movie["movie_year"] + "</p>" +
        "<p class='sub-info'>Director: " + movie["movie_director"] + "</p>" +
        "<p class='sub-info'>Rating: " + (movie["rating"] ? movie["rating"] : 0.0) + "</p>" +
        "<p class='sub-info'>Genres: " + (genreHtml || "N/A") + "</p>" +
        `<button id='cartbutton-${movieId}' class='page-button' onclick="addToCart('${movieId}')">Add to Cart</button>`);

    console.log("handleResult: populating star and genre table from resultData");

    // Populate the movie table
    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");

    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 0; i < movie["stars"].length; i++) {
        let star = movie["stars"][i];
        let rowHTML = "";
        rowHTML += "<tr><th>";
        rowHTML += `<a href='single_star.html?id=${star["star_id"]}'>${star["star_name"]}</a>`;
        rowHTML += "</th></tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }
}


function returnToMovieList() {
    console.log("RETURN TO MOVE LIST")

    jQuery.ajax({
        method: "GET",
        url: "api/get-state",
        success: function (state) {

            let params = new URLSearchParams();

            if (state.mode === "genre") {
                params.set("id", state.value);
            } else if (state.mode === "title") {
                params.set("char", state.value);
            } else {
                // restore search parameters
                if (state.searchTitle) params.set("title", state.searchTitle);
                if (state.searchYear) params.set("year", state.searchYear);
                if (state.searchDirector) params.set("director", state.searchDirector);
                if (state.searchStar) params.set("star", state.searchStar);
            }

            params.set("sort", state.sort || "title");
            params.set("order", state.order || "ASC");
            params.set("secSort", state.secSort || "rating");
            params.set("secOrder", state.secOrder || "DESC");
            params.set("page", state.page || 1);
            params.set("limit", state.limit || 20);

            window.location.href = "movie_list.html?" + params.toString();
        },
        error: function () {
            window.location.href = "movie_list.html";
        }
    });
}

function addToCart(movieId) {
    jQuery.ajax({
        method: "POST",
        url: "api/shopping-cart",
        data: {
            action: "add",
            movieId: movieId
        },
        success: function () {
            console.log("Added to cart: " + movieId);
            let button = jQuery("#cartbutton-" + movieId);
            button.text("Added to cart!");
            setTimeout(() => button.text("Add to Cart"), 1000);
        },
        error: function (err) {
            console.log("Add to cart error", err);
        }
    });
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let movieId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-movie?id=" + movieId, // Setting request url, which is mapped by SingleMovieServlet
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleMovieServlet
});