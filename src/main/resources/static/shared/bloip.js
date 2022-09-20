
$(document).ready(function() {
    pollInboxTotal();
});

function initBasicChecks() {
    if (!sessionStorage.checkedForInitialInboxAlert) {
        var inboxTotal = Number(document.getElementById("stash").getAttribute("inboxTotal"));
        if (inboxTotal > 0 ) {
            var message;
            if (inboxTotal > 1) {
                message = "You have " + inboxTotal + " audio responses to your comment(s).\r\n Would you like to see?"
            } else {
                message = "You have an audio response to your comment.\r\n Would you like to see?"
            }
            if (confirm(message)) {
                window.location.href = "/inbox";
            } else {
                //Do nothing for now....
            }
        }
        sessionStorage.checkedForInitialInboxAlert = true;
    }
}

var Inbox = {
    alert: false,
    total: undefined,
    isAlerting: false,
    total:undefined,
    lostFocus:  false,
    alertToggle : false,
    alertTitle: "",
    alertHref: "",
    intervalRef: {}
}

/** detect focus change...but so far doesn't seem needed
function windowLoad() {
    window.onfocus = function () {
        Inbox.lostFocus = false;
        console.log("FOCUS");
        //TODO: clearNotification state
    };
    window.onblur = function () {
        Inbox.lostFocus= true;
        console.log("BLURRR");
    };
} **/

function pollInboxTotal() {
    console.log("Polling inbox...." + Inbox.total);
    $.get("/inbox-total", function(data, status) {
        if (Inbox.total === undefined) {
            Inbox.total = Number(data);
            setTimeout(pollInboxTotal, 1000);

        } else {
            var responseCount = Number(data);
            if (Inbox.total < responseCount) {
                $("#inbox-total-value").text('(' + responseCount + ')');
                $("#inboxTotalAlertSymbol").html("<img src='/images/alert.png' width='40' height='40'/>");
                localPushNotify();
                playBloipAudio();
                startInboxAlertCycle();
            }
            else {
                setTimeout(pollInboxTotal, 5000);
            }
            Inbox.total = responseCount;
        }
    });
}

function playBloipAudio() {
    console.log("bloip audio played.")
    var audio = new Audio('/sounds/horse.mp3');
    audio.type = "audio/mpeg";
    audio.play().catch(function(e) {
        console.log(e);
    });
}

function startInboxAlertCycle() {
    Inbox.isAlerting = true;
    Inbox.alertTitle = document.title;
    Inbox.alertHref  = $("#favicon").attr("href");

    Inbox.intervalRef = setInterval(function() {
        if(!Inbox.alertToggle) {
            Inbox.alertToggle = true;
            document.title   = 'NEW INBOX MESSAGE';
            $("#favicon").attr("href","/images/notifications-icon.png");

        } else {
            Inbox.alertToggle = false;
            document.title    = Inbox.alertTitle;
            $("#favicon").attr("href", Inbox.alertHref);
        }
    }, 500);
}

//TODO: The local push makes sense if the page is open. Just need to make sure not to spam the user and to make sure a remote
//TODO: notification is not triggered.
function localPushNotify() {
    if (Notification.permission === "granted") {
        new Notification(
                "NEW INBOX MESSAGE",
                { badge: "/images/favicon.ico"},
                { image: "/images/favicon.ico"},
                { requireInteraction: true}
            );
    }
}

function subscribeToWebPushNotifications() {
    Notification.requestPermission().then(function(permission) {
        if (permission === "granted") {
            webPushSubscriptionHelper();
        }
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
                                localStorage.setItem("webpush", "yes");
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
