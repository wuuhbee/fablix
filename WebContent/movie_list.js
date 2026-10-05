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
$(document).ready(function() {
    displayCurrentSearchParameters();
    updateSortDisplay();
})

function updateSortDisplay() {
    let params = new URLSearchParams(window.location.search);

    if (params.has("sort")) {
        let first_sort = params.get("sort");
        $("#first-sort").val(first_sort);
        currentViewState.sort = first_sort;
    }
    if (params.has("order")) {
        let first_order = params.get("order");
        $(`input[name='first-order'][value='${first_order}']`).prop('checked', true);
        currentViewState.order = first_order;
    }

    if (params.has("secSort")) {
        let sec_sort = params.get("secSort");
        $("#second-sort").val(sec_sort);
        currentViewState.secSort = sec_sort;
    }
    if (params.has("secOrder")) {
        let sec_order = params.get("secOrder")
        $(`input[name='second-order'][value='${sec_order}']`).prop('checked', true);
        currentViewState.secOrder = sec_order;
    }
}

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

let currentViewState = {
    mode: "",
    value: "", // genreID, firstChar
    sort: "title",
    order: "ASC",
    secSort: "rating",
    secOrder: "DESC"
};


// for saving when single url clicked
function saveAndNavigate(href) {
    jQuery.ajax({
        method: "POST",
        url: "api/save-state",
        data: {
            mode: currentViewState.mode,
            value: currentViewState.value,
            sort: currentViewState.sort,
            order: currentViewState.order,
            secSort: currentViewState.secSort,
            secOrder: currentViewState.secOrder,
            page: page,
            limit: limit,
            searchTitle: title || "",
            searchYear: year || "",
            searchDirector: director || "",
            searchStar: star || ""
        },
        success: function () {
            window.location.href = href;
        },
        error: function () {
            window.location.href = href;
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
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleResult(resultData) {

    if (resultData.length === 0) {
        page = page - 1;
        window.location.href = buildUrl(window.location.pathname, page, limit);
        return;
    }

    console.log("handleResult: populating movie table from resultData");

    // populate the movie info h3
    // find the empty h3 body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");
    movieTableBodyElement.empty();

    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 0; i < resultData.length; i++) { // no longer Math.min(20, resultData.length)
        let rowHTML = "";
        rowHTML += "<tr>";

        let movieLink = `<a href='#' onclick="saveAndNavigate('single_movie.html?id=${resultData[i]["id"]}'); return false;">${resultData[i]["title"]}</a>`;
        rowHTML += "<th>" + movieLink + "</th>";

        rowHTML += "<th>" + resultData[i]["year"] + "</th>";
        rowHTML += "<th>" + resultData[i]["director"] + "</th>";
        rowHTML += "<th>" + resultData[i]["rating"] + "</th>";
        rowHTML += "<th>";

        // if first 3 genre
        if (resultData[i]["genres"]) {
            rowHTML += resultData[i]["genres"].join(" | ");
        }
        rowHTML += "</th>";

        // if first 3 star
        rowHTML += "<th>";
        if (resultData[i]["stars"]) {
            let starLinks = resultData[i]["stars"].map(star => {
                return `<a href='#' onclick="saveAndNavigate('single_star.html?id=${star["id"]}'); return false;">${star["name"]}</a>`;
            });
            rowHTML += starLinks.join(", ");
        }
        rowHTML += "</th>";

        // cart
        rowHTML += `<th><button id='cartbutton-${resultData[i]["id"]}' class='page-button' onclick="addToCart('${resultData[i]["id"]}')">Add to Cart</button></th>`;

        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }

    jQuery("#next_button").prop("disabled", resultData.length < limit);
}


function displayCurrentSearchParameters(){
    let params = new URLSearchParams(window.location.search);

    let searchTitle = params.get("title");
    let searchYear = params.get("year");
    let searchDirector = params.get("director");
    let searchStar = params.get("star");

    if (searchTitle) jQuery("#currently-searching-title").text("Current: " + searchTitle);
    if (searchYear) jQuery("#currently-searching-year").text("Current: " + searchYear);
    if (searchDirector) jQuery("#currently-searching-director").text("Current: " + searchDirector);
    if (searchStar) jQuery("#currently-searching-star").text("Current: " + searchStar);
}

function removeDisplayOfCurrentSearchParameters() {
    jQuery("#currently-searching-title").text("");
    jQuery("#currently-searching-year").text("");
    jQuery("#currently-searching-director").text("");
    jQuery("#currently-searching-star").text("");
}

function getMovieByGenres(genreId) {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: `api/genre-browse?id=${encodeURIComponent(genreId)}&sort=${currentViewState.sort}&order=${currentViewState.order}&page=${page}&limit=${limit}`,
        success: (resultData) => handleResult(resultData)
    });
    saveState();
}

function getMovieByTitleChar(firstChar) {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: `api/title-browse?char=${encodeURIComponent(firstChar)}&sort=${currentViewState.sort}&order=${currentViewState.order}&page=${page}&limit=${limit}`,
        success: (resultData) => handleResult(resultData)
    });
    saveState();
}

document.addEventListener("DOMContentLoaded", function() {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/get-genres",
        success: (resultData) => setupGenreBrowse(resultData)
    });
    setupTitleBrowse();
    setupSearch();
});

function setupGenreBrowse(resultData) {
    const container = document.getElementById("genre-container");
    container.innerHTML = ""; // Clear existing contents

    resultData.forEach(genre => {
        let a = document.createElement("a");
        a.textContent = genre.name;
        a.className = "browse-content";
        a.onclick = (e) => {
            e.preventDefault();
            updateCurrentModeToGenre(genre.name);
            getMovieByGenres(genre.name); // Triggers your movie list fetch
            saveState();
        };
        container.appendChild(a);
    });
}


function setupTitleBrowse() {
    const container = document.getElementById("alphabet-container");
    const rows = [
        "0123456789".split(""),
        "ABCDEFGHIJKLM".split(""),
        "NOPQRSTUVWXYZ".split("")
    ];

    rows.forEach(row => {
        let rowDiv = document.createElement("div");
        row.forEach((char, index) => {
            let a = document.createElement("a");
            a.textContent = char;
            a.className = "browse-content";
            a.onclick = (e) => {
                e.preventDefault();
                updateCurrentModeToTitle(char);
                getMovieByTitleChar(char);
                saveState();
            }
            rowDiv.appendChild(a);

            if (index < row.length - 1) {
                rowDiv.appendChild(document.createTextNode("|"));
            }
        });
        container.appendChild(rowDiv);
    });
}

// function toggleSort(sort) {
//     if (currentViewState.sort === sort) {
//         currentViewState.order = (currentViewState.order === "ASC") ? "DESC" : "ASC";
//     }
//     else {
//         currentViewState.secSort = currentViewState.sort;
//         currentViewState.secOrder = currentViewState.order;
//
//         currentViewState.sort = sort;
//         currentViewState.order = (sort === "rating") ? "DESC" : "ASC";
//     }
//     updateArrows();
//     updateCurrentState();
//     saveState();
// }

function updateCurrentModeToGenre(id) {
    currentViewState.mode = 'genre';
    currentViewState.value = id;
    removeDisplayOfCurrentSearchParameters();
}

function updateCurrentModeToTitle(firstChar) {
    currentViewState.mode = 'title';
    currentViewState.value = firstChar;
    removeDisplayOfCurrentSearchParameters();
}

function updateCurrentModeToSearch() {
    currentViewState.mode = 'search';
}

// function updateArrows() {
//     const titleIcon = document.getElementById("titleIcon");
//     const ratingIcon = document.getElementById("ratingIcon");
//
//     if (currentViewState.sort === "title") {
//         titleIcon.className = (currentViewState.order === "ASC") ? "fa-solid fa-sort-up" : "fa-solid fa-sort-down";
//         titleIcon.style.color = "#00aaff"; // Highlight active sort
//     } else {
//         titleIcon.className = (currentViewState.secOrder === "ASC") ? "fa-solid fa-sort-up" : "fa-solid fa-sort-down";
//     }
//
//     if (currentViewState.sort === "rating") {
//         ratingIcon.className = (currentViewState.order === "ASC") ? "fa-solid fa-sort-up" : "fa-solid fa-sort-down";
//         ratingIcon.style.color = "#00aaff";
//     } else {
//         ratingIcon.className = (currentViewState.secOrder === "ASC") ? "fa-solid fa-sort-up" : "fa-solid fa-sort-down";
//     }
// }

function applyMultiSort() {
    currentViewState.sort = $("#first-sort").val();
    currentViewState.order = $("input[name='first-order']:checked").val();
    currentViewState.secSort = $("#second-sort").val();
    currentViewState.secOrder = $("input[name='second-order']:checked").val();

    updateCurrentState();
    saveState();
}

function updateCurrentState() {
    let servletUrl = "";
    let params = new URLSearchParams();

    if (currentViewState.mode === 'genre') {
        servletUrl = "api/genre-browse";
        params.append("id", currentViewState.value);
    } else if (currentViewState.mode === 'title') {
        servletUrl = "api/title-browse";
        params.append("char", currentViewState.value);
    } else if (currentViewState.mode === 'search') {
        servletUrl = "api/search";
        if (title) params.append("title", title);
        if (year) params.append("year", year);
        if (director) params.append("director", director);
        if (star) params.append("star", star);
    } else if (currentViewState.mode === 'gen-search') {
        servletUrl = "api/gen-search";
        params.append("q", currentViewState.value);
    }

    params.append("sort", currentViewState.sort);
    params.append("order", currentViewState.order);
    params.append("secSort", currentViewState.secSort);
    params.append("secOrder", currentViewState.secOrder);
    params.append("page", page);
    params.append("limit", limit);

    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: `${servletUrl}?${params.toString()}`,
        success: (data) => handleResult(data)
    });

    saveState();
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Search parameters from URL
let title = getParameterByName("title");
let year = getParameterByName("year");
let director = getParameterByName("director");
let star = getParameterByName("star");

// session browse parameters
let genreId = getParameterByName("id");
let titleChar = getParameterByName("char");

// pagination
let page = getParameterByName("page");
let limit = getParameterByName("limit");

if (!page) page = 1;
if (!limit) limit = 20;

// restore state from single pages url
let sortParam = getParameterByName("sort");
let orderParam = getParameterByName("order");
let secSortParam = getParameterByName("secSort");
let secOrderParam = getParameterByName("secOrder");

if (sortParam) currentViewState.sort = sortParam;
if (orderParam) currentViewState.order = orderParam;
if (secSortParam) currentViewState.secSort = secSortParam;
if (secOrderParam) currentViewState.secOrder = secOrderParam;

// reload browse
if (genreId) {
    updateCurrentModeToGenre(genreId);
    getMovieByGenres(genreId);
} else if (titleChar) {
    updateCurrentModeToTitle(titleChar);
    getMovieByTitleChar(titleChar);
}

let url = "api/movies"; // default MovieListServlet TODO: remove this?

if (title || year || director || star) {
    url = "api/search";
    updateCurrentModeToSearch();
}


function buildUrl(baseUrl, page, limit) {
    let newUrl = baseUrl + "?page=" + page + "&limit=" + limit;

    if (title) newUrl += "&title=" + title;
    if (year) newUrl += "&year=" + year;
    if (director) newUrl += "&director=" + director;
    if (star) newUrl += "&star=" + star;

    if (currentViewState.mode === "genre") newUrl += "&id=" + currentViewState.value;
    if (currentViewState.mode === "title") newUrl += "&char=" + currentViewState.value;
    if (currentViewState.mode === "gen-search") newUrl += "&q=" + currentViewState.value;

    newUrl += "&sort=" + currentViewState.sort;
    newUrl += "&order=" + currentViewState.order;
    newUrl += "&secSort=" + currentViewState.secSort;
    newUrl += "&secOrder=" + currentViewState.secOrder;

    return newUrl;
}


// Initial pg load url
url = buildUrl(url, page, limit);


// Makes the HTTP GET request and registers on success callback function handleResult
if (!genreId && !titleChar && (title || year || director || star)) { // added && to hide initial default movie list on load for now
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: url,
        success: (resultData) => handleResult(resultData)
    });
}

// Pagination button logic
jQuery("#next_button").click(function () {

    let nextPage = parseInt(page) + 1;

    page = nextPage;
    jQuery("#page_display").text("Page " + page);
    saveState();
    // window.location.href = buildUrl(window.location.pathname,
    //     nextPage, limit);
    updateCurrentState();
});

jQuery("#prev_button").click(function () {

    let prevPage = parseInt(page) - 1;
    if (prevPage < 1) prevPage = 1;

    page = prevPage;
    jQuery("#page_display").text("Page " + page);
    saveState();
    // window.location.href = buildUrl(window.location.pathname, prevPage, limit);
    updateCurrentState();
});

jQuery("#page_display").text("Page " + page);

jQuery("#limit_input").val(limit);

jQuery("#limit_input").on("change", function () {
    let newLimit = jQuery("#limit_input").val();
    limit = newLimit;
    saveState();
    // window.location.href = buildUrl(window.location.pathname, page, newLimit);
    updateCurrentState();
});

function saveState() {
    jQuery.ajax({
        method: "POST",
        url: "api/save-state",
        data: {
            mode: currentViewState.mode,
            value: currentViewState.value,
            sort: currentViewState.sort,
            order: currentViewState.order,
            secSort: currentViewState.secSort,
            secOrder: currentViewState.secOrder,
            page: page,
            limit: limit,
            searchTitle: title || "",
            searchYear: year || "",
            searchDirector: director || "",
            searchStar: star || ""
        },
    });
}

function updateCurrentModeToGenSearch() {
    currentViewState.mode = 'gen-search';
    currentViewState.value = document.getElementById("general-search-box").value.trim();
    removeDisplayOfCurrentSearchParameters();
}


function setupSearch() {
    const searchForm = document.getElementById("search-form");
    const searchField = document.getElementById("general-search-box");


    // autocomplete to search
    $('#general-search-box').autocomplete({
        lookup: function(query, doneCallback) {
            handleLookup(query, doneCallback);
        },
        onSelect: function(suggestion) {
            handleSelectSuggestion(suggestion);
        },
        minChars: 3, //3 character requirement
        deferRequestBy: 300,
    });

    searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const query = searchField.value.trim();

        if (query.length === 0) {
            return;
        }
        updateCurrentModeToGenSearch();
        getMovies(query);
        saveState();
    });
}

function handleLookup(query, doneCallback) {
    console.log("autocomplete initiated");
    console.log("sending AJAX request to backend Java Servlet");

    // if the query in SessionStorage check
    let cached = sessionStorage.getItem("autocomplete_" + query);
    if (cached) {
        console.log("returning cached result for query: " + query);
        doneCallback({ suggestions: JSON.parse(cached) });
        return;
    }

    jQuery.ajax({
        "method": "GET",
        //escape the query string to avoid errors caused by special characters
        "url": "movie-suggestion?query=" + escape(query),
        "success": function(data) {
            handleLookupAjaxSuccess(data, query, doneCallback);
        },
        "error": function(errorData) {
            console.log("lookup ajax error");
            console.log(errorData);
        }
    });
}

function handleLookupAjaxSuccess(data, query, doneCallback) {
    console.log("lookup ajax successful");

    var jsonData = JSON.parse(data);
    console.log(jsonData);

    //SessionStorage cache
    sessionStorage.setItem("autocomplete_" + query, JSON.stringify(jsonData));

    //autocomplete library callback
    // need "{suggestions: jsonData}" to satisfy the library response format
    doneCallback({ suggestions: jsonData });
}

function handleSelectSuggestion(suggestion) {
    console.log("Selected " + suggestion["value"] + " with ID " + suggestion["data"]["movieID"]);

    //redirect to single movie page
    saveAndNavigate("single_movie.html?id=" + suggestion["data"]["movieID"]);
}

function getMovies(query) {
    console.log("searching for movies");
    let newUrl = buildUrl('api/gen-search', page, limit);
    jQuery.ajax({
        method: "GET",
        url: newUrl,
        success: (movies) => handleResult(movies)
    });
}