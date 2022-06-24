package com.bloip.controllers

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Controller
class HomeController(@Autowired val discussionService: DiscussionService, @Autowired val applicationProperties: ApplicationProperties) {
    @GetMapping("/")
    fun index(model: Model, @RequestParam(required = false) p: Int?): String {
        val discussions: Page<Discussion> = discussionService.getPage(p)
        model["discussions"] = discussions

        setPaginationParameters(model, p, discussions.size)
        return "index"
    }

    //TODO: Needs unit tests and should probably be moved into a utility/helper class.
    fun setPaginationParameters(model: Model, inputCurrentPage: Int?, numberOfResults: Int)  {
        var currentPage = inputCurrentPage
        if (currentPage != null) {
            model["p"] = currentPage
        } else {
            currentPage = 0
        }
        if (currentPage > 0) {
            model["previous"]    = true
            model["previousUrl"] = "/?p=${currentPage - 1}"
        }
        if (numberOfResults >= applicationProperties.discussionsPerPage) {
            model["next"]    = true
            model["nextUrl"] = "/?p=${currentPage + 1}"
        }
    }

    /** Can also use the following **/
    // <a th:href="@{/teams/{id}(id=${row.id})}" th:text="#{edit.team}"></a>
}