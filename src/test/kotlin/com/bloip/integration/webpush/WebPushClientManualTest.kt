package com.bloip.integration.webpush

import com.bloip.jobs.webpush.client.WebPushClient
import org.junit.jupiter.api.Test
import java.net.http.HttpResponse

/**
 * Created by Usman Mutawakil on 9/25/22.
 */
class WebPushClientManualTest {
    fun sendData() {
        val response: HttpResponse<String> = WebPushClient().push(
            publicKey            = "BC0VwAmu1vMIk2Mf34AFTKwYLNXvDAxM7D8m56t_dVVCr_jwsND7voE5mc-80AG0wK-7o5JrtSFNWa8koR-1G5E",
            privateKey           = "{Add a private key}",
            subscriptionEndpoint = "https://updates.push.services.mozilla.com/wpush/v2/gAAAAABjL3I-nTOBfb6qng0eBegkEc3a8fLH8z5R9uixoCxwKpkElpTtLnLMjkeQkA1k_CcS2qESg5GjKIT9pTn_mmV2ph9iuIC4u64ZbFWLlnubA4S73kSaI-bO2bMznWg5FrmBUrTN6qgrP6UGDkOBAi8CeZlPB0Y6lNcVDQ6n28eAFQNmx94"
        )

        //Chrome 2
        //https://fcm.googleapis.com/fcm/send/d2i4c5ckEmc:APA91bHUyJPXHM5i5OuzTn6dgkLDyDDKOdRRr_dXkM4ucLdkIFPSAif2NXYM51m0RZA0OkDHS9WPwyGpx0pebt1V3DqBresvYwHmDtouFpOBefZsG1m5nk1jxEcsJ1RaOIUvd2qksNus

        //Chrome
        //https://fcm.googleapis.com/fcm/send/fjjyW2GD0eg:APA91bHzAvpr4LUoLBVbHNy8k9XtDZFpayNkBb1_0L1GWw8ffwagyngCc4bZG1gyc8BTBogvKl1TvzJCHRyVrQOEk1pGHtobbXJsCBieFkPJTB_xNumpndNpS6sZfxD5sSWyduj__v6N

        //Firefox
        //https://updates.push.services.mozilla.com/wpush/v2/gAAAAABjL3I-nTOBfb6qng0eBegkEc3a8fLH8z5R9uixoCxwKpkElpTtLnLMjkeQkA1k_CcS2qESg5GjKIT9pTn_mmV2ph9iuIC4u64ZbFWLlnubA4S73kSaI-bO2bMznWg5FrmBUrTN6qgrP6UGDkOBAi8CeZlPB0Y6lNcVDQ6n28eAFQNmx94

        println("HTTP Response: ${response.statusCode()}")
        println("Response: ${response.body()}")

        if (!(response.statusCode() == 201 || response.statusCode() == 200)) {
            RuntimeException(response.body()).printStackTrace()
        }
    }

    @Test
    fun run() {

    }
}