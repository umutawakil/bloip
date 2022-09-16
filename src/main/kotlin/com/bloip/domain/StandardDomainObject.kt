package com.bloip.domain

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 9/8/22.
 *
 * TODO: This is suppose to be the super class for the domain objects
 */
@MappedSuperclass
open class StandardDomainObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open val id: Long = 0

    override fun equals(other: Any?) : Boolean {
        if (other == null) {
            throw NullPointerException("Equal comparison to null value")
        }
        return this.id == (other as StandardDomainObject).id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}