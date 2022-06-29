

Bloip.StateMachine = function() {
    var states            = [];
    var position          = -1;
    var self              = this;
    var initializedStates = new Set();
    var previousStateData = {};

    this.next = function(previousStateData) {
        $(document).ready(function() {
            console.log("Transitioning to new state");
            if(position >= 0) {
                console.log("Hiding existing state: " + states[position].getName());
                states[position].hide(self);
            }
            position++;

            console.log("Initializing state: " + states[position].getName());
            if(!initializedStates.has(position))  {
                console.log("Registering events for state: " + states[position].getName());
                states[position].initEvents(self);
                initializedStates.add(position);
            } else {

            }

            console.log("Showing UI....");
            states[position].show(self, previousStateData);

            console.log("Running arbitrary state code...");
            states[position].run(self, previousStateData);

            console.log("State " + states[position].getName() +" fully initialized");
        });
    };

    this.reset = function() {
        states[position].hide();
        position = -1;

        self.next();
    };

    this.addState = function(newState) {
        if(!newState) {
            throw new Error("Error: Attempting to add empty state: " + newState);
        }
        states.push(newState);
    };
}

