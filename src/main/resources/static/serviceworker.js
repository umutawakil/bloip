/** Send a notification after a push event is detected **/
self.addEventListener('push', (e) => {
    var options = {
        body: "Somebody has responded to your post.",
        badge: "/images/favicon.ico",
        image: "/images/favicon.ico",
        requireInteraction: true
    };
    self.registration.showNotification("NEW INBOX MESSAGE", options);
});

/** Take user to the inbox after they have clicked on the notification **/
self.addEventListener('notificationclick', function(event) {
    event.notification.close();
    clients.openWindow('/inbox');
}, false);