package com.bloip.controllers

import com.bloip.domain.Discussion
import com.bloip.services.DiscussionService
import com.bloip.services.NotificationService
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
        @Autowired val discussionService: DiscussionService,
        @Autowired val notificationService: NotificationService
    )
    {
        @GetMapping("/")
        fun index(model: Model, @RequestParam(required = false) p: Int?, httpSession: HttpSession): String {

            val userId: Long = httpSession.getAttribute("userId") as Long
            val discussions: List<Discussion> = discussionService.getPage(p)

            model["discussions"] = discussions
            model["inboxTotal"] = notificationService.getInboxTotal(userId)
            setPaginationParameters(
                model            = model,
                inputCurrentPage = p,
                hasResults       = discussions.isNotEmpty()
            )

            return "index"
        }

        //TODO: Needs unit tests and should probably be moved into a utility/helper class.
        fun setPaginationParameters(model: Model, inputCurrentPage: Int?, hasResults: Boolean)  {
            var currentPage = inputCurrentPage ?: 0

            if (currentPage > 0) {
                model["previous"]    = true
                model["previousUrl"] = "/?p=${currentPage - 1}"
            }

            //TODO: The pageable sdk is trash. Its size method doesn't work so you must check for empty.
            //TODO: A better paginations scheme is a must.
            if (hasResults) {
                model["next"]    = true
                model["nextUrl"] = "/?p=${currentPage + 1}"
            }
        }
        /** Can also use the following **/
        // <a th:href="@{/teams/{id}(id=${row.id})}" th:text="#{edit.team}"></a>
}