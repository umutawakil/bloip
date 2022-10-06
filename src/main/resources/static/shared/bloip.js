
$(document).ready(function() {
    initBasicChecks();
    //pollInboxTotal(); //Is this worth it? Better to tell people to leave the page open in the background
});

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
                //localPushNotify();
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

