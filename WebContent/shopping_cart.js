
function handleResult(resultData) {

    console.log("handleResult: populating cart table from resultData");

    let cartTableBodyElement = jQuery("#cart_table_body");
    cartTableBodyElement.empty();

    let total = 0;

    for (let i = 0; i < resultData.length; i++) {
        let item = resultData[i];
        let subtotal = item["price"] * item["quantity"];
        total += subtotal;

        let rowHTML = "";
        rowHTML += "<tr id='row-" + item["id"] + "'>";
        rowHTML += "<td>" + item["title"] + "</td>";
        rowHTML += "<td>$" + item["price"].toFixed(2) + "</td>";

        rowHTML += "<td>";
        rowHTML += "<button class='page-button' onclick=\"updateQuantity('" + item["id"] + "', " + (item["quantity"] - 1) + ")\">-</button>";
        rowHTML += " " + item["quantity"] + " ";
        rowHTML += "<button class='page-button' onclick=\"updateQuantity('" + item["id"] + "', " + (item["quantity"] + 1) + ")\">+</button>";
        rowHTML += "</td>";

        rowHTML += "<td>$" + subtotal.toFixed(2) + "</td>";

        rowHTML += "<td>";
        rowHTML += "<button class='page-button' onclick=\"deleteItem('" + item["id"] + "')\">Delete</button>";
        rowHTML += "</td>";
        rowHTML += "</tr>";

        cartTableBodyElement.append(rowHTML);
    }

    jQuery("#cart_total").text("Total: $" + total.toFixed(2));
}


function updateQuantity(movieId, newQuantity) {
    console.log("updateQuantity: movieId=" + movieId + " newQuantity=" + newQuantity);

    jQuery.ajax({
        method: "POST",
        url: "api/shopping-cart",
        data: {
            action: "update",
            movieId: movieId,
            quantity: newQuantity
        },
        success: function () {
            // reload cart after update
            loadCart();
        },
        error: function (err) {
            console.log("update error!!", err);
        }
    });
}


function deleteItem(movieId) {
    console.log("deleteItem: movieId=" + movieId);

    jQuery.ajax({
        method: "POST",
        url: "api/shopping-cart",
        data: {
            action: "delete",
            movieId: movieId
        },
        success: function () {
            loadCart();
        },
        error: function (err) {
            console.log("delete error!!", err);
        }
    });
}

function loadCart() {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/shopping-cart",
        success: (resultData) => handleResult(resultData)
    });
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

loadCart();