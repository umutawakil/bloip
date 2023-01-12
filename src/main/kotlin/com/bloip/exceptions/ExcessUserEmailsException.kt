package com.bloip.exceptions

/**
 * Created by Usman Mutawakil on 12/30/22.
 */
class ExcessUserEmailsException : RuntimeException {
    //TODO: Needs translation
    constructor() : super("Too many emails sent today.")
}