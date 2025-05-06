function handlePostResult(resultData) {
    if (resultData["success"]) {
        $(location).attr('href', 'main')
    } else {
        let msgSection = jQuery("#message-section")
        msgSection.empty()
        msgSection.append(
            "<p style='color:red'>" + resultData["message"] + "</p>"
        )
    }
}

jQuery("#login-form").submit(function (event) {
    event.preventDefault();
    
    // Check if reCAPTCHA is completed
    const recaptchaResponse = grecaptcha.getResponse();
    if (!recaptchaResponse) {
        // Show error if reCAPTCHA not completed
        jQuery("#recaptcha-error").show();
        return;
    }
    
    // Hide error message if reCAPTCHA completed
    jQuery("#recaptcha-error").hide();
    
    // Get form data and add reCAPTCHA response
    let formData = $(this).serialize();
    formData += "&g-recaptcha-response=" + recaptchaResponse;
    
    jQuery.ajax({
        dataType: "json",
        data: formData,
        method: "POST",
        url: "api/login",
        success: (resultData) => handlePostResult(resultData)
    });
})