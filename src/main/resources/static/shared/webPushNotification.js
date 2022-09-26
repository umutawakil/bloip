
function explainWebPushNotificationsIfNeeded() {
    if (!('Notification' in window)) {
        alert("Your current browser doesn't support web notification.\r\n" +
            " In the future if you want to be notified when someone responds then try a desktop computer with Google chrome or FireFox.");
        return;
    }
    if(Notification.permission === "denied" ) { //TODO: User has already subscribed for notifications. Should we refresh their key?
        alert("You've blocked web notifications from this site so you won't be directly notified in your browser when someone responds.\r\n" +
            " If you change your mind and wish to allow notifications then you will need to manually change your settings. \r\n" +
            " You can send us a message on twitter @BloipApp if you need help on how to do that.");
        return;
    }
    if(Notification.permission === "granted" && (localStorage.getItem("webpush") !== null)) { //TODO: User has already subscribed for notifications. Should we refresh their key?
        console.log("User already requested and uploaded web push subscription info. Skipping this step.");
        return;
    }

    /** New users below **/

    var message = "We want to notify you in your browser when someone has responded to your post after you have left the site.\r\n" +
        "We don't use email or phone numbers so this is the only way to reach you directly. Please allow web notifications on the next screen. \r\n" +
        "Your identity will continue to remain anonymous but you can also follow us on twitter @BloipApp";
    if(confirm(message)) {
        subscribeToWebPushNotifications();
    }
}

function subscribeToWebPushNotifications() {
    Notification.requestPermission().then(function(permission) {
        if (permission === "granted") {
            webPushSubscriptionHelper();
        }
    });
}

function webPushSubscriptionHelper() {
    registerServiceWorker();

    navigator.serviceWorker.ready.then(
        function (serviceWorkerRegistration) {
            var vapidPublicKey = $("#stash").attr("applicationServerKey");
            const options = { userVisibleOnly: true, applicationServerKey: vapidPublicKey};
            console.log(JSON.stringify(options));
            serviceWorkerRegistration.pushManager.subscribe(options).then(function (pushSubscription) {
                    console.log("Data to Send: " + JSON.stringify(pushSubscription));
                    console.log("Subscription json: " + pushSubscription.toJSON());

                    var key  = pushSubscription.getKey("p256dh")
                    var auth = pushSubscription.getKey("auth")
                    var formData = new FormData()
                    formData.append("key", btoa(String.fromCharCode.apply(null, new Uint8Array(key))));
                    formData.append("auth", btoa(String.fromCharCode.apply(null, new Uint8Array(auth))));
                    formData.append("expirationTime", pushSubscription.expirationTime ? "" + pushSubscription.expirationTime :"");
                    formData.append("endpoint", pushSubscription.endpoint);

                    $.ajax({
                        type: "post",
                        url: "/web-push-subscription-info",
                        contentType: false,
                        processData: false,
                        data: formData,
                        error: function (xhr, textStatus, error) {
                            alert("Failed to reply to discussion. " + textStatus + " " + error);
                        },
                        success: function (data) {
                            if (data == 1) {
                                //TODO
                                // localStorage.setItem("webpush", "yes");
                                console.log("Webpush set")
                            } else {
                                console.log("Error uploading web push subscription details");
                            }
                        }
                    });
                }, function (error) {
                    console.error(error);
                    alert(error);
                }
            );
        });
}

function registerServiceWorker() {
    navigator.serviceWorker.register('/serviceworker.js', {scope: '/'}).then(function(){
        console.log("Service worker registered");
    }).catch(function(error) {
        console.log(error);
        alert(error);
    });
}
