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

    console.log("handleResult: populating star info from resultData");

    // populate the star info h3
    // find the empty h3 body by id "star_info"
    let starInfoElement = jQuery("#star_info");

    // append two html <p> created to the h3 body, which will refresh the page
    starInfoElement.append("<h1 class='main-name'>" + resultData[0]["star_name"] + "</h1>");
    if (resultData[0]["star_dob"]){
        starInfoElement.append("<p class='sub-info'>Date Of Birth: " + resultData[0]["star_dob"] + "</p>");
    } else{
        starInfoElement.append("<p class='sub-info'>Date Of Birth: N/A</p>");
    }

    console.log("handleResult: populating movie table from resultData");

    // Populate the star table
    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");

    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        let movieString = `<a href='single_movie.html?id=${resultData[i]["movie_id"]}'>${resultData[i]["movie_title"]}</a>`;
        rowHTML += "<th>" + movieString + "</th>";
        rowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
        rowHTML += "<th>" + resultData[i]["movie_director"] + "</th>";
        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }
}

function returnToMovieList() {
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

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let starId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-star?id=" + starId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});