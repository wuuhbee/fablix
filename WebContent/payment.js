$(document).ready(function() {
    console.log("getting cart total");
    $.ajax(
        {
            url: "api/get-cart",
            method: "GET",
            success: (data) => parseCartTotal(data)
        }
    )
});

function parseCartTotal(resultData) {
    let total = resultData["price"];
    $("#total-price").text("Total: $" + parseFloat(total).toFixed(2));
}

function handlePaymentCheck(resultData) {
    console.log("validating user payment info");

    if (resultData["status"] === "success") {
        window.location.replace("confirmation.html");
    } else {
        console.log("show error message");
        console.log(resultData["message"]);
        let errorMsg = jQuery("#payment-error-message")
        errorMsg.text(resultData["message"]);
    }
}

function returnToMainPage() {
    window.location.replace("movie_list.html");
}

let paymentForm = $("#paymentForm");

function submitPayment(formSubmitEvent){
    formSubmitEvent.preventDefault();
    console.log("submit payment info");
    $.ajax(
        {
            url: `api/payment`,
            method: "POST",
            data: paymentForm.serialize(),
            success: resultData => handlePaymentCheck(resultData)
        }
    );
}

paymentForm.submit(submitPayment);