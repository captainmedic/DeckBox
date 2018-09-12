package com.r0adkll.deckbuilder.arch.ui.features.decks


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import com.r0adkll.deckbuilder.R
import com.r0adkll.deckbuilder.arch.data.AppPreferences
import com.r0adkll.deckbuilder.arch.data.remote.model.ExpansionPreview
import com.r0adkll.deckbuilder.arch.domain.features.decks.model.Deck
import com.r0adkll.deckbuilder.arch.domain.features.editing.repository.EditRepository
import com.r0adkll.deckbuilder.arch.ui.Shortcuts
import com.r0adkll.deckbuilder.arch.ui.components.BaseFragment
import com.r0adkll.deckbuilder.arch.ui.components.ListRecyclerAdapter
import com.r0adkll.deckbuilder.arch.ui.features.browse.SetBrowserActivity
import com.r0adkll.deckbuilder.arch.ui.features.deckbuilder.DeckBuilderActivity
import com.r0adkll.deckbuilder.arch.ui.features.decks.DecksUi.State
import com.r0adkll.deckbuilder.arch.ui.features.decks.adapter.DecksRecyclerAdapter
import com.r0adkll.deckbuilder.arch.ui.features.decks.adapter.Item
import com.r0adkll.deckbuilder.arch.ui.features.decks.di.DecksModule
import com.r0adkll.deckbuilder.arch.ui.features.exporter.MultiExportActivity
import com.r0adkll.deckbuilder.arch.ui.features.home.di.HomeComponent
import com.r0adkll.deckbuilder.arch.ui.features.testing.DeckTestingActivity
import com.r0adkll.deckbuilder.internal.analytics.Analytics
import com.r0adkll.deckbuilder.internal.analytics.Event
import com.r0adkll.deckbuilder.util.DialogUtils
import com.r0adkll.deckbuilder.util.DialogUtils.DialogText.*
import com.r0adkll.deckbuilder.util.ScreenUtils
import com.r0adkll.deckbuilder.util.ScreenUtils.smallestWidth
import com.r0adkll.deckbuilder.util.extensions.isVisible
import com.r0adkll.deckbuilder.util.extensions.plusAssign
import com.r0adkll.deckbuilder.util.extensions.snackbar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_decks.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class DecksFragment : BaseFragment(), DecksUi, DecksUi.Intentions, DecksUi.Actions {

    override var state: State = State.DEFAULT

    @Inject lateinit var renderer: DecksRenderer
    @Inject lateinit var presenter: DecksPresenter
    @Inject lateinit var preferences: AppPreferences
    @Inject lateinit var editor: EditRepository

    private val viewPreview: Relay<ExpansionPreview> = PublishRelay.create()
    private val dismissPreview: Relay<Unit> = PublishRelay.create()
    private val shareClicks: Relay<Deck> = PublishRelay.create()
    private val duplicateClicks: Relay<Deck> = PublishRelay.create()
    private val deleteClicks: Relay<Deck> = PublishRelay.create()
    private val testClicks: Relay<Deck> = PublishRelay.create()

    private lateinit var adapter: DecksRecyclerAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_decks, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = DecksRecyclerAdapter(activity!!, shareClicks, duplicateClicks, deleteClicks,
                testClicks, dismissPreview, viewPreview)
        adapter.setOnItemClickListener(object : ListRecyclerAdapter.OnItemClickListener<Item> {
            override fun onItemClick(v: View, item: Item, position: Int) {
                if (item is Item.DeckItem) {
                    Analytics.event(Event.SelectContent.Deck(item.deck.id))

                    // Update shortcuts
                    Shortcuts.addDeckShortcut(v.context, item.deck)

                    // Generate a new session and pass to builder activity
                    disposables += editor.createSession(item.deck)
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                startActivity(DeckBuilderActivity.createIntent(activity!!, it))
                            }
                }
            }
        })

        adapter.setEmptyView(empty_view)



        val layoutManager = if (smallestWidth(ScreenUtils.Config.TABLET_10)) {
            StaggeredGridLayoutManager(6, StaggeredGridLayoutManager.VERTICAL) as RecyclerView.LayoutManager
        } else {
            val lm = GridLayoutManager(activity, 2)
            lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val item = adapter.items[position]
                    return when(item) {
                        is Item.Preview -> 2
                        else -> 1
                    }
                }
            }
            lm
        }

        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        fab.setOnClickListener { _ ->
            if (quickTip.isVisible()) {
                quickTip.hide(fab)
            }

            Analytics.event(Event.SelectContent.Action("new_deck"))

            // Generate a new session and pass to builder activity
            disposables += editor.createSession()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        startActivity(DeckBuilderActivity.createIntent(activity!!, it, true))
                    }
        }

        if (preferences.quickStart) {

            // Fix for Fabric#212
            disposables += Observable.timer(300L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        quickTip.show(fab, R.string.deck_quickstart_message)
                    }
            preferences.quickStart = false
        }

        disposables += shareClicks
                .subscribe {
                    Analytics.event(Event.SelectContent.Action("export_decklist"))
                    val intent = MultiExportActivity.createIntent(activity!!, it)
                    startActivity(intent)
                }

        disposables += testClicks
                .subscribe {
                    Analytics.event(Event.SelectContent.Action("test_decklist"))
                    val intent = DeckTestingActivity.createIntent(activity!!, it.id)
                    startActivity(intent)
                }

        disposables += viewPreview
                .subscribe { preview ->
                    startActivity(SetBrowserActivity.createIntent(activity!!, preview.code))
                }

        renderer.start()
        presenter.start()
    }


    override fun onDestroy() {
        renderer.stop()
        presenter.stop()
        super.onDestroy()
    }


    override fun setupComponent() {
        getComponent(HomeComponent::class)
                .plus(DecksModule(this))
                .inject(this)
    }


    override fun render(state: State) {
        this.state = state
        renderer.render(state)
    }


    override fun dismissPreview(): Observable<Unit> = dismissPreview.doOnNext { Analytics.event(Event.SelectContent.Action("dismiss_preview", "Forbidden Light")) }
    override fun shareClicks(): Observable<Deck> = shareClicks
    override fun duplicateClicks(): Observable<Deck> = duplicateClicks
    override fun deleteClicks(): Observable<Deck> = deleteClicks.flatMap { deck ->
        DialogUtils.confirmDialog(activity!!,
                Resource(R.string.dialog_delete_deck_title),
                Resource(R.string.dialog_delete_deck_message, deck.name),
                R.string.action_delete,
                R.string.action_cancel)
                .flatMap { if (it) Observable.just(deck) else Observable.empty() }
    }


    override fun showLoading(isLoading: Boolean) {
        empty_view.setLoading(isLoading)
    }


    override fun showError(description: String) {
        snackbar(description)
    }


    override fun hideError() {
    }


    override fun showItems(items: List<Item>) {
        adapter.showItems(items)
    }

    override fun balanceShortcuts(decks: List<Deck>) {
        Shortcuts.balanceShortcuts(activity!!, decks)
    }

    companion object {

        fun newInstance(): DecksFragment = DecksFragment()
    }
}