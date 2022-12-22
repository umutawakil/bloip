package com.bloip.integration.utils

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.gargoylesoftware.htmlunit.html.DomElement
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.junit.jupiter.api.Assertions.assertEquals

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

        fun getLinkFromEmail(s3: AmazonS3, emailBucket: String, beforeSearchText: String, afterSearchText: String) : String? {
            /** Get the token out of the s3 bucket and assert that it's present/ grab the link to the token**/
            var checks = 0
            while(s3.listObjects(emailBucket).objectSummaries.size ==0 && checks < 10) {
                Thread.sleep(1000)
                checks++
            }
            val objectList = s3.listObjects(emailBucket)
            if ( objectList.objectSummaries.size == 0) {
                return null
            }
            //assertEquals(1, objectList.objectSummaries.size)

            var s3Object: S3Object? = null
            for(s in objectList.objectSummaries) {
                s3Object = s3.getObject(emailBucket, s.key)
            }
            println("ObjectList Size: " + objectList.objectSummaries.size)
            println("Object: " + s3Object!!.key)

            val lines: List<String>  = s3Object.objectContent.bufferedReader().lines().toList()
            s3.deleteObject(emailBucket, s3Object.key) //Causes problems in subsequent emails if this sticks around

            return findBetween(beforeSearchText, afterSearchText, lines[lines.size - 1])
            /*return lines[lines.size - 1].
            replace(beforeSearchText, "").
            replace(afterSearchText, "")*/
        }

        private fun findBetween(start: String, stop:String, input: String): String  {
            val positionX = input.indexOf(start) + start.length
            val positionY = input.indexOf(stop)
            return input.substring(positionX, positionY)
        }

        fun numEmailsPresent(s3: AmazonS3, emailBucket: String) : Int {
            /** Get the token out of the s3 bucket and assert that it's present/ grab the link to the token**/
            var checks = 0
            while(s3.listObjects(emailBucket).objectSummaries.size ==0 && checks < 10) {
                Thread.sleep(1000)
                println("Waiting for email....")
                checks++
            }
            val objectList = s3.listObjects(emailBucket).objectSummaries
            val size = objectList.size

            for(s in objectList) {
                s3.deleteObject(emailBucket, s.key)
            }
            return size
        }
    }
}