
function handleLoginResult(resultData) {
    console.log("handleLoginResults: validating user login info");

    console.log("handle login response");
    console.log(resultData);
    console.log(resultData["status"]);

    if (resultData["status"] === "success") {
        window.location.replace("/cs122b_spring_cs_122b_flicks_the_sql_war/_dashboard/main.html");
    } else {
        console.log("show error message");
        console.log(resultData["message"]);
        let errorMsg = jQuery("#login-error-message")
        errorMsg.text(resultData["message"]);
    }
}

$(document).ready(function() {
    console.log("Dashboard JS is active");

    // Use the ID of the button specifically
    $("#loginButton").on("click", function() {
        console.log("Button clicked, starting AJAX...");

        $.ajax({
            method: "POST",
            url: "/cs122b_spring_cs_122b_flicks_the_sql_war/api/_dashboard-login",
            data: $("#employeeLoginForm").serialize(),
            success: (resultData) => {
                console.log("Response received:", resultData);
                handleLoginResult(resultData);
            },
        });
    });
});