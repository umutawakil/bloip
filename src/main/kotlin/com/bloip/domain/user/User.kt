package com.bloip.domain.user

import com.bloip.domain.StandardDomainObject
import com.bloip.domain.user.authentication.AuthenticationUserDetail
import java.util.Date
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Entity
@Table(name = "user")
class User : StandardDomainObject
{
    @Column
    var censured: Boolean = false

    @Column
    var censureDate: Date? = null

    @Column
    var emailDisabled: Boolean = false

    /** Don't cascade delete. The DB is using cascade on delete for its foreign keys **/
    @OneToOne(
        optional = true,
        fetch = FetchType.EAGER,
        cascade = [
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.MERGE,
            CascadeType.DETACH
        ]
    )
    @JoinColumn(name = "user_detail_id", referencedColumnName = "user_id", nullable = true)
    var authenticationUserDetail: AuthenticationUserDetail? = null

    constructor()

    /** Code below is a temporary quick fix for limiting users to 10 discussions a day **/
    @Column
    var discussionCreationCount: Int = 0

    @Column
    var firstDiscussionCreationInLastDay: Date? = null

    @Version
    private val version = 0

    @Transient
    fun getEmail() : String? {
        if (this.authenticationUserDetail == null) return null

        return this.authenticationUserDetail!!.username
    }

    fun resetDiscussionCreationWindow() {
        this.discussionCreationCount = 0
        this.firstDiscussionCreationInLastDay = Date()
    }

    fun isEmailNotifiable() : Boolean {
        return ((this.authenticationUserDetail != null) && !this.emailDisabled)
    }
}