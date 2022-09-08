package com.bloip.controllers

import com.bloip.domain.Discussion
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import com.bloip.structures.BumpStack
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.*
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Controller
class HomeController(
        @Autowired val discussionService: DiscussionService,
        @Autowired val inboxService: InboxService
    )
    {
        @GetMapping("/")
        fun index(model: Model, @RequestParam(required = false) o: Long?,
                  @RequestParam(required = false) d: Int?, httpSession: HttpSession): String {
            val userId: Long = WebUtil.getUserIdFromSession(httpSession)

            val page: BumpStack.Page<Long, Discussion> = if( d == null || d >= 0 ) {
                discussionService.getNextPage(offsetKey = o)
            }  else {
                if (o == null) {
                    BumpStack.Page(previousOffsetKey = null, nextOffsetKey = null, values = emptyList())
                } else {
                    discussionService.getPreviousPage( offsetKey = o)
                }
            }

            model["discussions"] = page.values
            WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
            WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)
            model["inboxTotal"]  = inboxService.getInboxTotal(userId)

            println("hasPrevious: ${page.previousOffsetKey}, hasNext: ${page.nextOffsetKey}")

            val date = Date(System.currentTimeMillis())
            println("Date: ${date}");

            return "index"
        }
}