import { record } from "./vmsg.js";

class WasmWrapper {
    constructor()
    {
        this.wasm;
        this.stream;
        this.blob;
        this.objectUrl;
    }

    /*getAudioPermission() {
        const getUserMedia = navigator.mediaDevices && navigator.mediaDevices.getUserMedia
            ? function(constraints) {
                return navigator.mediaDevices.getUserMedia(constraints);
            }
            : function(constraints) {
                const oldGetUserMedia = navigator.webkitGetUserMedia || navigator.mozGetUserMedia;
                if (!oldGetUserMedia) {
                    return Promise.reject(new Error("getUserMedia is not implemented in this browser"));
                }
                return new Promise(function(resolve, reject) {
                    oldGetUserMedia.call(navigator, constraints, resolve, reject);
                });
            };
        getUserMedia({audio: true}).then(stream => {
            console.log("Audio permission verified. Closing temporary stream.");
            this.closeStream(stream);
        }).catch(error => {
            if (confirm("You need to allow microphone access in order to use your microphone!") == true) {
                window.location = "/";
            } else {
                window.location = "/";
            }
        });
    }*/

    setWasm(inputWasm) {
        this.wasm = inputWasm;
    }

    setStream(inputStream) {
        console.log("Stream set....");
        this.stream = inputStream;
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
        this.closeStream();
    }

    closeStream(x) {
        if(x) {
            this.stream = x;
        }
        this.stream.getTracks().forEach(track => track.stop());
    }
}

export var WASM = new WasmWrapper();