package com.bloip.controllers.admin

import com.bloip.domain.discussion.Discussion
import com.bloip.structures.BumpStack
import com.bloip.utilities.WebUtil
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Created by Usman Mutawakil on 10/30/22.
 */

@Controller
@RequestMapping("/valkyrie/")
class AdminController(
) {
    @GetMapping("/")
    fun index(model: Model, @RequestParam(required = false) o: Long?,
              @RequestParam(required = false) d: Int?): String {
        return "index"
    }

    /**
     * TODO: Admin stuff
     *
    Comments
    <p>Censor comment</p><BR/>
    <p>Censor all user comments</p><BR/>

    Discussion
    <p>Delete discussion</p><BR/>
    <p>Delete all user discussions</p><BR/>

    Blocking
    <p>Block User</p><BR/>
    <p>Block IP</p>
    **/
}