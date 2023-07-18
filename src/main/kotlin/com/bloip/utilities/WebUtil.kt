package com.bloip.utilities

import com.bloip.domain.user.User
import org.springframework.ui.Model
import org.springframework.ui.set
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/7/22.
 */
class WebUtil {
    init {}
    companion object {

        fun getIpFromRequest(request: HttpServletRequest) : String {
            return request.getHeader("X-Forwarded-For")
        }

        fun getUserIdFromSession(httpSession: HttpSession?) : User.UserId? {
            if (httpSession == null) {
                return null
            }
            return httpSession.getAttribute("userId") as User.UserId?
        }

        fun getUserFromSession(httpSession: HttpSession) : User? {
            return User.findById(
                userId = getUserIdFromSession(
                    httpSession = httpSession
                )
            )
        }
        fun safeSetModelAttribute(model: Model, attribute:String, value: Any?) {
            if (value != null) {
                model.set(attributeName = attribute, attributeValue = value)
            }
        }
    }
}