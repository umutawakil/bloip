package com.bloip.integration

import org.springframework.boot.test.context.SpringBootTest

/**
 * Created by Usman Mutawakil on 9/8/22.
 */
@SpringBootTest
class ConcurrencyTest {
    //TODO Multiple Users/threads in the same discussion replying to a discussion, observing no notifications for 100 ms. then subscribing and observing notifications for 100ms
    //TODO: For each user at the end check the number of inbox notifications is correct
    //TODO: Verify the inbox order for each user and pagination sequence.
    //TODO: Verify the discussion order and pagination.

    //TODO: A station for inbox(pagination,unsubscribe/subscribe,delete), creation, replies all running with X number of users where each user is represented by a thread
    //TODO: Should numberically name content so we can see the user that created it and the order in the name. Should help get a sense of how its effecting the discussions
}