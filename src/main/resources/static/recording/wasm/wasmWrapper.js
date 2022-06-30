import { record } from "./vmsg.js";

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
        record( {wasmURL: "/recording/wasm/vmsg.wasm"}).then(blob => {
            console.log("Recorded MP3", blob);
            this.objectUrl = window.URL.createObjectURL(blob);
            this.blob = blob;
            document.getElementById("previewControl").src = this.objectUrl;
        });
    }

    startRecording() {
        this.wasm.startRecording();
    }
    stopRecording() {
        this.wasm.finish();
    }
}

export var WASM = new WasmWrapper();