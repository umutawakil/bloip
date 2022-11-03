package com.bloip.utilities

import org.springframework.ui.Model
import org.springframework.ui.set
import java.net.URL
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/7/22.
 */
class WebUtil {

    init {

    }
    companion object {
        private val baseURL: String = System.getenv("BASE_URL")

        fun getUserIdFromSession(httpSession: HttpSession) : Long {
            return httpSession.getAttribute("userId") as Long
        }
        fun safeSetModelAttribute(model: Model, attribute:String, value: Any?) {
            if (value != null) {
                model.set(attributeName = attribute, attributeValue = value)
            }
        }
    }
}