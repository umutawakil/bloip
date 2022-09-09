package com.bloip.domain

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 9/8/22.
 *
 * TODO: This is suppose to be the super class for the domain objects
 */

//@Entity
open abstract class StandardDomainObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open val id: Long = 0

    constructor()

    override fun equals(inputOtherObject: Any?) : Boolean {
        if (inputOtherObject == null) {
            throw NullPointerException("Equal comparison to null value")
        }
        val other: StandardDomainObject = inputOtherObject as StandardDomainObject
        if (other.id == null) {
            throw NullPointerException("Equal comparison against null id")
        }
        return this.id == other.id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}