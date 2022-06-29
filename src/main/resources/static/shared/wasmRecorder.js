import { record } from "./vmsg.js";

/*$(document).ready(function() {
    $("#test-button").click(function(){
        record( {wasmURL: "/shared/vmsg.wasm"}).then(blob => {
            console.log("Recorded MP3", blob);
            var objectURL  = window.URL.createObjectURL(blob);
            alert(objectURL);
        });
    });
});*/

class WasmWrapper {
    constructor()
    {
        this.wasm;
        this.blob;
        this.objectUrl;
    }

    setWasm(inputWasm) {
        this.wasm = inputWasm;
    }

    init() {
        record( {wasmURL: "/shared/vmsg.wasm"}).then(blob => {
            console.log("Recorded MP3", blob);
            this.objectUrl = window.URL.createObjectURL(blob);
            this.blob = blob;
            document.getElementById("previewControl").src = this.objectUrl;
        });
    }

    getObjectUrl() {
        return this.objectUrl;
    }

    startRecording() {
        this.wasm.startRecording();
    }
    stopRecording() {
        this.wasm.finish();
    }
}

export var WASM = new WasmWrapper();