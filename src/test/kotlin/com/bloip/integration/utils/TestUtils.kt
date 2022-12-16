package com.bloip.integration.utils

import com.gargoylesoftware.htmlunit.html.DomElement
import com.gargoylesoftware.htmlunit.html.HtmlPage

/**
 * Created by Usman Mutawakil on 12/12/22.
 */
class TestUtils {
    companion object {
        fun getElementById(page: HtmlPage, id: String) : DomElement? {
            return try {
                page.getHtmlElementById(id)
            } catch (e: Exception) {
                null
            }
        }
    }
}