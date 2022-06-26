import { record } from "./vmsg.js";

$(document).ready(function() {

    $("#test-button").click(function(){
        record( {wasmURL: "/shared/vmsg.wasm"}).then(blob => {
            console.log("Recorded MP3", blob);

            var objectURL  = window.URL.createObjectURL(blob);
            alert(objectURL);
            $("#test-link").attr("href", objectURL);

            // Can be used like this:
            //
            // const form = new FormData();
            // form.append("file[]", blob, "record.mp3");
            // fetch("/upload.php", {
            //   credentials: "include",
            //   method: "POST",
            //   body: form,
            // }).then(resp => {
            // });
        });
    });
});
