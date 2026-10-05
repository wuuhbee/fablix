
function handleLoginResult(resultData) {
    console.log("handleLoginResults: validating user login info");

    console.log("handle login response");
    console.log(resultData);
    console.log(resultData["status"]);

    if (resultData["status"] === "success") {
        window.location.replace("movie_list.html");
    } else {
        console.log("show error message");
        console.log(resultData["message"]);
        let errorMsg = jQuery("#login-error-message")
        errorMsg.text(resultData["message"]);
    }
}

let loginForm = $("#loginForm");

function submitLoginForm(formSubmitEvent) {
    formSubmitEvent.preventDefault();
    console.log("submit login form");
    $.ajax(
        {
            url: `api/login`,
            method: "POST",
            data: loginForm.serialize(),
            success: resultData => handleLoginResult(resultData)
        }
    );
}

loginForm.submit(submitLoginForm);