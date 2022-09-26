/** Send a notification after a push event is detected **/
self.addEventListener('push', (e) => {
    console.log("Service Worker Called!!!!!!");
    var options = {
        body: "Somebody has responded to your post.",
        title: "See new message in inbox",
        icon:  "/images/msc/short-logo.png",
        requireInteraction: true
    };
    self.registration.showNotification("NEW COMMENT", options);
    console.log("Service Worker Show notification executed!!!!!!");
});

/** Take user to the inbox after they have clicked on the notification **/
self.addEventListener('notificationclick', function(event) {
    event.notification.close();
    clients.openWindow('/inbox');
}, false);