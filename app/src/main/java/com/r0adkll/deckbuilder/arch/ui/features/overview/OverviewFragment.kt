package com.r0adkll.deckbuilder.arch.ui.features.overview

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ftinc.kit.kotlin.utils.bindLong
import com.ftinc.kit.kotlin.utils.bundle
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import com.r0adkll.deckbuilder.R
import com.r0adkll.deckbuilder.arch.domain.features.cards.model.EvolutionChain
import com.r0adkll.deckbuilder.arch.domain.features.cards.model.PokemonCard
import com.r0adkll.deckbuilder.arch.domain.features.editing.model.Session
import com.r0adkll.deckbuilder.arch.ui.components.BaseFragment
import com.r0adkll.deckbuilder.arch.ui.components.EditCardIntentions
import com.r0adkll.deckbuilder.arch.ui.features.carddetail.CardDetailActivity
import com.r0adkll.deckbuilder.arch.ui.features.deckbuilder.di.SessionId
import com.r0adkll.deckbuilder.arch.ui.features.overview.adapter.OverviewRecyclerAdapter
import com.r0adkll.deckbuilder.arch.ui.features.overview.di.OverviewModule
import com.r0adkll.deckbuilder.arch.ui.features.overview.di.OverviewableComponent
import com.r0adkll.deckbuilder.arch.ui.widgets.PokemonCardView
import com.r0adkll.deckbuilder.internal.analytics.Analytics
import com.r0adkll.deckbuilder.internal.analytics.Event
import com.r0adkll.deckbuilder.util.ScreenUtils.orientation
import com.r0adkll.deckbuilder.util.extensions.plusAssign
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_overview.*
import javax.inject.Inject


class OverviewFragment : BaseFragment(), OverviewUi, OverviewUi.Intentions, OverviewUi.Actions {

    override var state: OverviewUi.State = OverviewUi.State.DEFAULT

    private val sessionIdByIntent by bindLong(EXTRA_SESSION_ID, Session.NO_ID)
    private val editCardIntentions: EditCardIntentions = EditCardIntentions()
    private val cardClicks: Relay<PokemonCardView> = PublishRelay.create()

    @JvmField @field:[Inject SessionId] var sessionIdByInject: Long = Session.NO_ID
    @Inject lateinit var presenter: OverviewPresenter
    @Inject lateinit var renderer: OverviewRenderer

    private lateinit var adapter: OverviewRecyclerAdapter

    private val sessionId
        get() = if (sessionIdByInject != Session.NO_ID) {
            sessionIdByInject
        } else {
            sessionIdByIntent
        }


    @SuppressLint("RxSubscribeOnError")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        state = state.copy(sessionId = sessionId)

        adapter = OverviewRecyclerAdapter(activity!!, cardClicks, editCardIntentions)
        adapter.setEmptyView(emptyView)
        val spanCount = if (orientation(ORIENTATION_LANDSCAPE)) 7 else 4
        val layoutManager = androidx.recyclerview.widget.GridLayoutManager(activity!!, spanCount)
        layoutManager.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val item = adapter.items[position]
                return item.size
            }
        }
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        disposables += cardClicks.subscribe {
            CardDetailActivity.show(activity!!, it, sessionId)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }


    override fun setupComponent() {
        getComponent(OverviewableComponent::class)
                .plus(OverviewModule(this))
                .inject(this)

//        addDelegate(RendererFragmentDelegate(renderer))
//        addDelegate(PresenterFragmentDelegate(presenter))
    }

    override fun onStart() {
        super.onStart()
        renderer.start()
        presenter.start()
    }


    override fun onStop() {
        presenter.stop()
        renderer.stop()
        super.onStop()
    }


    override fun render(state: OverviewUi.State) {
        this.state = state
        renderer.render(state)
    }


    override fun addCards(): Observable<List<PokemonCard>> {
        return editCardIntentions.addCardClicks
                .doOnNext { Analytics.event(Event.SelectContent.Action("edit_add_card")) }
    }


    override fun removeCard(): Observable<PokemonCard> {
        return editCardIntentions.removeCardClicks
                .doOnNext { Analytics.event(Event.SelectContent.Action("edit_remove_card")) }
    }


    override fun showCards(cards: List<EvolutionChain>) {
        adapter.setCards(cards)
    }


    override fun showLoading(isLoading: Boolean) {
        emptyView.setLoading(isLoading)
    }


    override fun showError(description: String) {
        emptyView.emptyMessage = description
    }


    override fun hideError() {
        emptyView.setEmptyMessage(R.string.empty_deck_overview)
    }


    companion object {
        const val TAG = "OverviewFragment"
        private const val EXTRA_SESSION_ID = "OverviewFragment.SessionId"


        fun newInstance(sessionId: Long): OverviewFragment {
            val fragment = OverviewFragment()
            fragment.arguments = bundle { EXTRA_SESSION_ID to sessionId }
            return fragment
        }
    }
}
