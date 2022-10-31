package com.bloip.controllers

import com.bloip.domain.discussion.Discussion
import com.bloip.services.DiscussionService
import com.bloip.structures.BumpStack
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Controller
class HomeController(
        @Autowired val discussionService: DiscussionService
    )
    {
        @GetMapping("/")
        fun index(model: Model, @RequestParam(required = false) o: Long?,
                  @RequestParam(required = false) d: Int?): String {

            val page: BumpStack.Page<Long, Discussion> = getPage(d = d, o = o)

            model["discussions"] = page.values

            WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
            WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)

            return "index"
        }

        fun getPage(d: Int?, o: Long?) : BumpStack.Page<Long, Discussion> {
            return if ( d == null || d >= 0 ) {
                discussionService.getNextPage(
                    offsetKey = o
                )
            }  else {
                if (o == null) {
                    BumpStack.Page(
                        previousOffsetKey = null,
                        nextOffsetKey = null,
                        values = emptyList()
                    )
                } else {
                    discussionService.getPreviousPage(
                        offsetKey = o
                    )
                }
            }
        }

        @GetMapping("/error-test")
        fun error(
            model: Model, @RequestParam(required = false) o: Long?,
            @RequestParam(required = false) d: Int?, httpSession: HttpSession
        ): String {
            throw RuntimeException("This is an exception")
        }
}