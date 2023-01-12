package com.bloip.domain

import java.io.Serializable
import javax.persistence.*
import kotlin.properties.Delegates

/**
 * Created by Usman Mutawakil on 9/8/22.
 *
 * TODO: This is suppose to be the super class for the domain objects
 */
@MappedSuperclass
open class StandardDomainObject: Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open val id: Long = -1 //Seems to be a bug in hibernate/kotlin. Without -1 cascading save doesn't work on new objects saved with many-to-one. A new Discussion save will complain
    //that the topic needs to be saved first even though it already has.

    override fun equals(other: Any?) : Boolean {
        if (other == null) {
            return false
        }
        return this.id == (other as StandardDomainObject).id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}