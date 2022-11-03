
window.addEventListener("popstate", function(event) {
    if(!event.state) {
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
    globalStateMachine.transitionToStateByPosition(previousPosition);
});

var Bloip = {};

var globalStateMachine;
Bloip.StateMachine = function() {
    var finished          = false;
    var states            = [];
    var position          = -1;
    var self              = this;
    var initializedStates = new Set();
    //var previousStateData = {};

    this.skipStateOnBack = function(desiredPosition) {
        if(desiredPosition < 0) {
            window.location.href = "/";
            return;
        }
        console.log("DESIRED POS: " + desiredPosition)
        var desiredState = states[desiredPosition];

        return desiredState.skipOnBackButton === true;
    }

    this.complete = function () {
        finished = true;
    }

    this.isComplete = function () {
        return finished;
    }

    this.transitionToStateByPosition = function(newPosition) {
        console.log("Currently at position number: " + position);
        console.log("Currently at state name: " + states[position].getName());
        console.log("Transitioning to position: " + newPosition);
        //states[position].hide(self)
        //position = -1;

        /*if(states[newPosition].getName()==="recording") {
            position = newPosition - 1;
            console.log("Skipping recording state to go back");
        } else {
            position = newPosition;
        }
        console.log("New Position: " + newPosition + ", state: "+states[position].getName());*/
        this.go(newPosition);
    };

    this.next = function(previousStateData) {
        console.log("Transitioning to new state");
        if(position >= 0) {
            states[position].hide(self);
        }
        position++;
        console.log("StateMachine Position: " + position);

        if(!initializedStates.has(position))  {
            states[position].initEvents(self);
            initializedStates.add(position);
        } else {

        }

        window.scrollTo(0, 0); /** This is mostly for mobile **/
        states[position].show(self, previousStateData);
        states[position].run(self, previousStateData);
        window.history.pushState({'statePosition': position}, window.location);
    };

    this.go = function(newPosition) {
        states[position].hide(self);
        position = newPosition;

        window.scrollTo(0, 0); /** This is mostly for mobile **/
        states[position].show(self);
        states[position].run(self);

        //Dont enqueue more states when jumping backwards. Use history.back() to work through the queue.
        //window.history.pushState({'statePosition': position}, window.location);
    };


    /** Remember offset starts at -1 so it takes 3 steps to skip back 2 states **/
    this.back = function(x) {
        states[position].hide();
        position = position - x;
        self.next();
    };

    this.addState = function(newState) {
        if(!newState) {
            throw new Error("Error: Attempting to add empty state: " + newState);
        }
        states.push(newState);
    };

    this.addStates = function(states) {
        states.forEach(function(x) {
            self.addState(x);
        })
    }
}

