package com.r0adkll.deckbuilder.arch.domain.features.remote


import com.r0adkll.deckbuilder.arch.domain.features.remote.model.ExpansionPreview
import com.r0adkll.deckbuilder.arch.domain.features.remote.model.ExpansionVersion
import com.r0adkll.deckbuilder.arch.domain.features.remote.model.Reprints
import com.r0adkll.deckbuilder.arch.domain.features.remote.model.SearchProxies


/**
 * Wrapper around Firebase Remote Configuration SDK
 */
interface Remote {

    /**
     * This is the versioning string for the latest expansion set offered by the api. It's format as
     * follows: <version_code>.<expansion_code> e.g. 1.sm7
     *
     * - version_code represents the version of the data that may change unrelated to new expansions (i.e. rotation legality changes)
     * - expansion_code represents the latest available expansion in the set (i.e. sm7 - Celestial Storm) which can indicate if a new expansion was added
     */
    val expansionVersion: ExpansionVersion?


    /**
     * This is the spec for an expansion preview card that appears on the deck list screen to tell
     * users about a new expansion that has been added and other information about it. It also attempts
     * to direct them to browse the expansion
     */
    val expansionPreview: ExpansionPreview?


    /**
     * This is a list of search proxy/aliases that better improve the search experience for the user
     */
    val searchProxies: SearchProxies?


    /**
     * This is a list of hashes for cards that are not in standard or expanded formats but have been
     * reprinted in format valid sets since.
     */
    val reprints: Reprints?


    /**
     * Check for update remote config values and update them if needed. Also set
     * remote configuration settings if needed
     */
    fun check()
}