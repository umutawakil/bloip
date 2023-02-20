
window.addEventListener("popstate", function(event) {
    try {
        if (!event.state) {
            window.location.href = "/";
            return;
        }
        var previousPosition = event.state.statePosition;
        if (globalStateMachine.skipStateOnBack(previousPosition) === true) {
            console.log("Skipping auto state");
            history.back();
            return;
        }

        console.log("POPSTATE: " + previousPosition);

        if (globalStateMachine.isComplete()) {
            window.location.href = "/";
            return;
        }
        globalStateMachine.go(previousPosition);

    } catch (e) {
        logUserEvent("Error-recording-popstate-error", e.message);
        alert($("#t-unexpected-error").text());
        window.location.href = "/";
    }
});

var Bloip = {};

var globalStateMachine;
Bloip.StateMachine = function() {
    var finished          = false;
    var states            = [];
    var position          = -1;
    var self              = this;
    var initializedStates = new Set();

    this.skipStateOnBack = function(desiredPosition) {
        try {
            if (desiredPosition < 0) {
                window.location.href = "/";
                return;
            }
            var desiredState = states[desiredPosition];
            return desiredState.skipOnBackButton === true;

        } catch (e) {
            logUserEvent("Error-recording-skipStateOnBack", e.message);
            alert($("#t-unexpected-error").text());
            window.location.href = "/";
        }
    };

    this.complete = function () {
        finished = true;
    };

    this.isComplete = function () {
        return finished;
    };

    this.next = function(previousStateData) {
        try {
            console.log("Transitioning to new state");
            if (position >= 0) {
                states[position].hide(self);
            }
            position++;
            console.log("State: " + states[position].getName() +", at position: " + position);
            if (!initializedStates.has(position)) {
                states[position].initEvents(self);
                initializedStates.add(position);
            } else {

            }

            window.scrollTo(0, 0);
            /** This is mostly for mobile **/
            states[position].show(self, previousStateData);
            states[position].run(self, previousStateData);
            window.history.pushState({'statePosition': position}, window.location);

        } catch (e) {
            logUserEvent("Error-recording-next", e.message);
            alert($("#t-unexpected-error").text());
            window.location.href = "/";
        }
    };

    this.go = function(newPosition) {
        try {
            states[position].hide(self);
            position = newPosition;

            window.scrollTo(0, 0);
            /** This is mostly for mobile **/
            states[position].show(self);
            states[position].run(self);

            //Dont enqueue more states when jumping backwards. Use history.back() to work through the queue.
            //window.history.pushState({'statePosition': position}, window.location);
        } catch (e) {
            logUserEvent("Error-recording-go", e.message);
            alert($("#t-unexpected-error").text());
            window.location.href = "/";
        }
    };


    /** Remember offset starts at -1 so it takes 3 steps to skip back 2 states **/
    this.back = function(x) {
        try {
            states[position].hide();
            position = position - x;
            self.next();
        } catch (e) {
            logUserEvent("Error-recording-back", e.message);
            alert($("#t-unexpected-error").text());
            window.location.href = "/";
        }
    };

    this.addState = function(newState) {
        if (!newState) {
            throw new Error("Error: Attempting to add empty state: " + newState);
        }
        states.push(newState);
    };

    this.addStates = function(states) {
        try {
            states.forEach(function (x) {
                self.addState(x);
            });
        } catch (e) {
            logUserEvent("Error-recording-addStates", e.message);
            alert($("#t-unexpected-error").text());
            window.location.href = "/";
        }
    };
}

