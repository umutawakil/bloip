package com.bloip.domain.discussion

import com.amazonaws.services.mediaconvert.model.*
import com.bloip.configuration.EnvironmentConfigs
import com.bloip.domain.localization.Country
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.discussion.value.YoutubeLink
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User
import com.bloip.services.audioconversion.AudioConversionRequestService
import com.bloip.utilities.DiscussionUtility
import org.springframework.ui.Model
import org.springframework.ui.set
import java.util.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion : StandardDomainObject {
    @Embedded
    var title: Title

    @Embedded
    val youtubeLink: YoutubeLink?

    @Column
    val ipAddress: String

    @Column
    var numberOfReplies: Int = 0

    @Column
    val creationTimestamp: Date

    @Column
    var updateTimestamp: Date

    @Column
    val userId: Long

    @Column
    var lastUserId: Long

    @Column
    var fileName: String

    @Column
    var needsConversion: Boolean

    @Column
    var audioConversionInProgress = false

    @Column
    var conversionJobId: String? = null

    @Column
    var censured: Boolean = false

    @ManyToOne(optional = true)
    @JoinColumn(name = "country_id", referencedColumnName = "id", nullable = false)
    val country: Country

   @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.MERGE,
            CascadeType.DETACH
        ],
       mappedBy = "discussion"
    )
    private val comments: MutableList<Comment> = mutableListOf()

    @Version
    val version = 0
    constructor(
        userId: Long,
        title: Title,
        ipAddress: String,
        fileName: String,
        youtubeLink: YoutubeLink? = null,
        needsConversion: Boolean,
        country: Country,
        duration: Int
    ) {
        this.userId            = userId
        this.lastUserId        = userId
        this.title             = title
        this.ipAddress         = ipAddress
        this.creationTimestamp = Date()
        this.updateTimestamp   = Date()
        this.fileName          = fileName
        this.youtubeLink       = youtubeLink
        this.needsConversion   = needsConversion
        this.country           = country

        this.addComment(
            userId          = userId,
            fileName        = fileName,
            trackNumber     = 0,
            ipAddress       = ipAddress,
            duration        = duration,
            needsConversion = needsConversion
        )
    }

    //TODO: Some annotations needed to highlight that this tricky looking thing does indeed have unit tests
    val audioUrl:  String
        get() = if (!censured) { DiscussionUtility.getPotentiallyConvertedFileLocation(
            needsConversion = this.needsConversion,
            fileName        = this.fileName
        ) } else {
            EnvironmentConfigs.mscCdn + "/sounds/horse.mp3"
        }

    //TODO: Some annotations needed to highlight that this tricky looking thing does indeed have unit tests
    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(language: Language): String {
        return "/d/" + this.id + "/l/" + language.code
    }

    fun getEnglishUrl() : String {
        return "/d/" + this.id + "/l/en"
    }

    fun addComment(userId: Long, fileName: String, trackNumber: Int, ipAddress: String, duration: Int, needsConversion: Boolean) : Discussion {
        this.comments.add(
            Comment(
                discussion      = this,
                userId          = userId,
                fileName        = fileName,
                trackNumber     = trackNumber,
                ipAddress       = ipAddress,
                duration        = duration,
                needsConversion = needsConversion
            )
        )
        return this
    }

    fun lastCommentNeedsConversion() : Boolean {
        return this.comments[this.comments.size - 1].needsConversion
    }

    fun convertLastComment(mediaConversionService: AudioConversionRequestService) {
        mediaConversionService.startConvertingAudioFile(
            discussion  = this,
            trackNumber = this.comments.size - 1
        )
    }

    fun censureTitle() : Discussion {
        this.title = Title("   ")
        return this
    }
    fun censureComment(trackNumber: Int) {
        if(trackNumber >= this.comments.size) return
        val comment: Comment = this.comments[trackNumber]
        comment.censured = true
    }

    fun censureUser(trackNumber: Int) {
        if(trackNumber >= this.comments.size) return

        censureComment(trackNumber=trackNumber)

        val user: User? = User.findById(this.comments[trackNumber].userId)
        if(user != null) {
            user.censureUser()
        }
    }

    fun displaySingleComment(model: Model, trackNumber: Int) {
        if (trackNumber >= this.comments.size) return
        model["comment"] = this.comments[trackNumber]
    }

    fun createJobSettings(awsUploadBucketName: String, trackNumber: Int): JobSettings {
        val keyName: String = this.comments[trackNumber].fileName
        val jobSettings = JobSettings()

        jobSettings.timecodeConfig = TimecodeConfig().withSource("ZEROBASED")

        val outputGroup = OutputGroup()
        outputGroup.name = "file-group-1"
        outputGroup.customName = "File Group"

        val audioDescription = AudioDescription()
        audioDescription.audioSourceName = "Audio Selector 1"
        audioDescription.codecSettings = AudioCodecSettings()
        audioDescription.codecSettings.codec = "AAC"
        audioDescription.codecSettings.aacSettings = AacSettings()
        audioDescription.codecSettings.aacSettings.bitrate = 96000
        audioDescription.codecSettings.aacSettings.codingMode = "CODING_MODE_2_0"
        audioDescription.codecSettings.aacSettings.sampleRate = 48000
        audioDescription.codecSettings.aacSettings.specification = "MPEG4"

        var output = Output()
        output.containerSettings = ContainerSettings()
        output.containerSettings.container = "MP4"
        output.setAudioDescriptions(listOf(audioDescription))
        outputGroup.setOutputs(listOf(output))

        outputGroup.outputGroupSettings = OutputGroupSettings()
        outputGroup.outputGroupSettings.type = "FILE_GROUP_SETTINGS"
        outputGroup.outputGroupSettings.fileGroupSettings = FileGroupSettings()
        outputGroup.outputGroupSettings.fileGroupSettings.destination =
            "s3://${awsUploadBucketName}/output/"
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings = DestinationSettings()
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings.s3Settings = S3DestinationSettings()
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings.s3Settings.accessControl =
            S3DestinationAccessControl()
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings.s3Settings.accessControl.cannedAcl =
            "PUBLIC_READ"
        jobSettings.setOutputGroups(listOf(outputGroup))

        val input = Input()
        val audioSelector = AudioSelector()
        audioSelector.defaultSelection = "DEFAULT"
        audioSelector.externalAudioFileInput = "s3://${awsUploadBucketName}/$keyName"
        //audioSelector.audioDurationCorrection    = "AUTO"
        input.audioSelectors = HashMap<String, AudioSelector>()
        input.audioSelectors["Audio Selector 1"] = audioSelector
        input.timecodeSource = "ZEROBASED"

        jobSettings.setInputs(listOf(input))

        return jobSettings
    }

    fun conversionJobStarted(trackNumber: Int, jobId: String) : Discussion {
        if (trackNumber > this.comments.size) {
            return this
        }
        this.comments[trackNumber].conversionJobId           = jobId
        this.comments[trackNumber].audioConversionInProgress = true
        return this
    }

    fun updatedConversionJobInfo(conversionJobInfo:MutableMap<String, Pair<Int, Discussion>>) {
        for(i in 0 until this.comments.size) {
            if (this.comments[i].conversionJobId != null) {
                conversionJobInfo.put(this.comments[i].conversionJobId!!, Pair(first = i, second = this))
            }
        }
    }

    fun conversionComplete(trackNumber: Int) : Discussion {
        if (trackNumber >= this.comments.size) {
            return this
        }
        this.comments[trackNumber].audioConversionInProgress = false
        this.comments[trackNumber].needsConversion = false
        return this
    }

    fun getView(start: Int, end: Int) : Any? {
        val normalizedEnd: Int = if (end > this.comments.size) { this.comments.size} else { end }
        val temp: MutableList<Comment> = mutableListOf()
        for(c in this.comments.subList(start, normalizedEnd)) {
            temp.add(c)
        }
        return temp
    }

    @Entity
    @Table(name = "comment")
    private class Comment : StandardDomainObject {
        @ManyToOne(
            optional = false,
            fetch = FetchType.LAZY,
            cascade = []
        )
        @JoinColumn(name = "discussion_id", referencedColumnName = "id", nullable = false)
        val discussion: Discussion

        @Column
        val fileName: String  //"https://www.w3schools.com/html/horse.mp3" //null

        @Column
        val trackNumber: Int

        @Column
        val userId: Long

        @Column
        var ipAddress:String

        @Column
        val duration: Int

        @Column
        var needsConversion: Boolean

        @Column
        var audioConversionInProgress = false

        @Column
        var conversionJobId: String? = null

        @Column
        var censured: Boolean = false

        @Version
        private val version = 0
        constructor(
            discussion: Discussion,
            userId: Long,
            fileName: String,
            trackNumber: Int,
            ipAddress: String,
            duration: Int,
            needsConversion: Boolean
        ) {
            this.discussion      = discussion
            this.userId          = userId
            this.fileName        = fileName
            this.trackNumber     = trackNumber
            this.ipAddress       = ipAddress
            this.duration        = duration
            this.needsConversion = needsConversion
        }

        val audioUrl: String
            get() = if (!censured) { DiscussionUtility.getPotentiallyConvertedFileLocation(
                needsConversion = this.needsConversion,
                fileName        = this.fileName
            ) } else {
                EnvironmentConfigs.mscCdn + "/sounds/horse.mp3"
            }
    }
}