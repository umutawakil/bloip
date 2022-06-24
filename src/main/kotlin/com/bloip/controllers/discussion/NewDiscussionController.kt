package com.bloip.controllers.discussion

import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.servlet.http.HttpServletRequest


/**
 * Created by Usman Mutawakil on 6/23/22.
 */
@Controller
class NewDiscussionController (@Autowired val discussionService: DiscussionService){
    @GetMapping("/new-discussion")
    fun get(): String {
        return "discussion/new-discussion"
    }

    @PostMapping("/new-discussion")
    @ResponseBody
    fun post(request: HttpServletRequest, @RequestParam("bloip.mp3") multipartFile: MultipartFile): String {
        val file = File("/Users/NtroduceMe/Downloads/test3.mp3")
        file.createNewFile()
        copyInputStreamToFile(multipartFile.inputStream, file)
        return "success"

        /*val filePart = request.getPart("bloip.mp3")
        println("File part size: " + filePart.size);

        val fileName = "test1.mp3"
        var p = 0
        for (part in request.parts) {
            part.write("/Users/NtroduceMe/Downloads/$fileName")
            p++
        }
        println("Number of Parts: ${p}")
        return "success";*/
    }

    fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        // append = false
        FileOutputStream(file, false).use { outputStream ->
            var read: Int
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (inputStream.read(bytes).also { read = it } != -1) {
                outputStream.write(bytes, 0, read)
            }
        }
    }
}