package com.bloip.utilities

import org.springframework.ui.Model
import org.springframework.ui.set
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/7/22.
 */
class WebUtil {
    companion object {
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