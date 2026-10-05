function handleMetadataResult(resultData) {
    const gridContainer = jQuery("#metadata-container");
    gridContainer.empty();

    resultData.forEach((table) => {
        let cardHtml = `
            <div class="col-xl-3 col-lg-4 col-md-6 mb-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-header bg-dark text-white">
                        <h5 class="card-title mb-0 text-capitalize">${table.tableName}</h5>
                    </div>
                    <div class="card-body p-0">
                        <table class="table table-striped table-sm mb-0">
                            <thead class="thead-light">
                                <tr>
                                    <th class="pl-3">Column</th>
                                    <th>Type</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${table.columns.map(col => `
                                    <tr>
                                        <td class="pl-3"><code>${col.columnName}</code></td>
                                        <td><small class="text-muted">${col.columnType}</small></td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>`;
        gridContainer.append(cardHtml);
    });
}

$(document).ready(function() {
   jQuery.ajax( {
       dataType: "json",
       method: "GET",
       url: "/cs122b_spring_cs_122b_flicks_the_sql_war/_dashboard/api/get-metadata",
       success: (results) => handleMetadataResult(results)
   });
});

function displaySuccessMessage(resultData) {
    console.log("handling success message");
    if (resultData["status"] === "success") {
        console.log("addition successful.")
        let newId = resultData["new_star_id"];
        jQuery("#success-message").text("Successfully added star with id: " + newId + "!");
    }
    else {
        jQuery("#error-message").text("Failed to Add Star!");
    }
}

function displayErrorMessage(resultData) {
    console.log("handling error message");
    jQuery("#error-message").text("Failed to Add Star!");
}

function addStar(formSubmit) {
    formSubmit.preventDefault();
    console.log("Add Star Form Submitted");
    $.ajax(
        {
            url: "/cs122b_spring_cs_122b_flicks_the_sql_war/_dashboard/api/add-star",
            method: "POST",
            data: $("#addStarForm").serialize(),
            success: resultData => displaySuccessMessage(resultData),
            error: resultData => displayErrorMessage(resultData)
        }
    );
}

function displayMovieResult(resultData) {
    if (resultData["status"] === "success") {
        jQuery("#movie-success-message").text(resultData["message"]);
        jQuery("#movie-error-message").text("");
    } else {
        jQuery("#movie-error-message").text(resultData["message"]);
        jQuery("#movie-success-message").text("");
    }
}

function displayMovieError(resultData) {
    jQuery("#movie-error-message").text("Failed to Add Movie!");
}

function addMovie(formSubmit) {
    formSubmit.preventDefault();
    console.log("Add Movie Form Submitted");
    $.ajax({
        url: "/cs122b_spring_cs_122b_flicks_the_sql_war/_dashboard/api/add-movie",
        method: "POST",
        data: $("#addMovieForm").serialize(),
        success: resultData => displayMovieResult(resultData),
        error: resultData => displayMovieError(resultData)
    });
}

jQuery("#addMovieForm").submit(addMovie);

jQuery("#addStarForm").submit(addStar);