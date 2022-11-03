package com.bloip.controllers.admin

import com.bloip.domain.discussion.Discussion
import com.bloip.structures.BumpStack
import com.bloip.utilities.WebUtil
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Created by Usman Mutawakil on 10/30/22.
 */
class AdminController {
    @GetMapping("/high-command")
    fun index(model: Model, @RequestParam(required = false) o: Long?,
              @RequestParam(required = false) d: Int?): String {
        return "index"
    }
}