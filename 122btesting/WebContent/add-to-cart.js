function handlePostResult(resultData) {
    let snackbar = document.getElementById("mySnackbar");
    let type = ""
    if (resultData["success"]) {
        type = "success"
        snackbar.className = "snackbar success show"
        snackbar.textContent = "The movie has been added to the shopping cart";
    } else {
        type = "error"
        snackbar.className = "snackbar error show"
        snackbar.textContent = resultData["message"];
    }
    setTimeout(function() {
        snackbar.className = snackbar.className.replace("show", "").replace(type, "");
    }, 3000);
}

function attachAddToCartHandler() {
    $('.add-to-cart-form').each(function() {
        $(this).submit(function (event) {
            event.preventDefault();
            let formData = $(this).serialize();
            jQuery.ajax({
                dataType: "json",
                data: formData,
                method: "POST",
                url: "api/add-to-cart",
                success: (resultData) => handlePostResult(resultData)
            });
        })
    })
}