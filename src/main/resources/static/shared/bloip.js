
$(document).ready(function() {
    initBasicChecks();
});

function previewPlay() {
    $("#preview-play-button").css("display", "none");
    document.getElementById("previewControl").play();
    $("#preview-stop-button").css("display", "inline");
}
function previewStop() {
    $("#preview-stop-button").css("display", "none");
    document.getElementById("previewControl").pause();
    document.getElementById("previewControl").currentTime = 0;
    $("#preview-play-button").css("display", "inline");
}

function initBasicChecks() {
    if (!sessionStorage.checkedForInitialInboxAlert) {
        getInboxTotal().then(function(inboxTotal) {
            if (inboxTotal > 0) {
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
        });
    }
}

function getInboxTotal() {
    return new Promise(function(resolve, reject) {
        $.get("/inbox-total", function (data, status) {
            resolve(data);
        }).catch(function(e) {
            console.log(e);
            reject(e);
        });
    });
}

/** Used to alert users in real time when a response to their message has been posted **/
function showInboxNotification(count) {
    $("#inbox-total-value").text('(' + count + ')');
    $("#inboxTotalAlertSymbol").html("<img src='/images/alert.png' width='40' height='40'/>");
    playBloipAudio();
    startInboxAlertCycle();
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

/*function pollInboxTotal() {
    console.log("Polling inbox...." + Inbox.total);
    $.get("/inbox-total", function(data, status) {
        if (Inbox.total === undefined) {
            Inbox.total = Number(data);
            setTimeout(pollInboxTotal, 60000);

        } else {
            var responseCount = Number(data);
            if (Inbox.total < responseCount) {
                $("#inbox-total-value").text('(' + responseCount + ')');
                $("#inboxTotalAlertSymbol").html("<img src='/images/alert.png' width='40' height='40'/>");
                playBloipAudio();
                startInboxAlertCycle();
            }
            else {
                setTimeout(pollInboxTotal, 5000);
            }
            Inbox.total = responseCount;
        }
    });
}*/


/** TODO: The code below is not in use. Theres a chance it will be replaced with replaced with email notifications **/
function initWebsocketConnection() {
    var url = new URL(window.location.href);

    console.log(JSON.stringify(url));

    var websocket = new WebSocket("wss://"+url.host+"/web-socket");
    websocket.onopen = function() {
        $.get("/ws-info", function (data) {
            websocket.send(data);
        }).catch(function(e) {
            console.log(e);
        });
    };

    websocket.onmessage = function(event) {
        showInboxNotification(event.data);
    }
}

function initNotificationsGenie() {
    const childWindow = window.open(
        "/child-notifications-window",
        "",
        "width=300,height=300"
    );
}