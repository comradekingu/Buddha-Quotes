/**

Buddha Quotes
Copyright (C) 2021  BanDev

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */

package org.bandev.buddhaquotes.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.WindowCompat.setDecorFitsSystemWindows
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
import org.bandev.buddhaquotes.R
import org.bandev.buddhaquotes.adapters.QuoteRecycler
import org.bandev.buddhaquotes.architecture.ListViewModel
import org.bandev.buddhaquotes.architecture.QuoteViewModel
import org.bandev.buddhaquotes.core.*
import org.bandev.buddhaquotes.custom.AddQuoteSheet
import org.bandev.buddhaquotes.custom.CustomiseListSheet
import org.bandev.buddhaquotes.databinding.ActivityListBinding
import org.bandev.buddhaquotes.items.Quote

/**
 * The activity where the user can see all the quotes they have in their
 * lists
 */

class ListActivity : LocalizationActivity(), QuoteRecycler.Listener {

    private lateinit var binding: ActivityListBinding
    private var toolbarMenu: Menu? = null
    private lateinit var quoteModel: QuoteViewModel
    private lateinit var listModel: ListViewModel
    private lateinit var list: MutableList<Quote>
    private var listId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAccentColour(this)

        with(window) {
            statusBarColor = Color.TRANSPARENT
            setNavigationBarColourDefault()
            setDarkStatusIcons()
        }
        setDecorFitsSystemWindows(window, false)

        // Setup view binding
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quoteModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(QuoteViewModel::class.java)

        listModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(ListViewModel::class.java)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressed() }
            applyInsetter {
                type(statusBars = true) {
                    margin(top = true)
                }
            }
        }
        with(binding.addQuote) {
            applyInsetter {
                type(navigationBars = true) {
                    margin(bottom = true)
                }
            }
            backgroundTintList = ColorStateList.valueOf(resolveColorAttr(R.attr.colorAccent))
            setOnClickListener { view ->
                Feedback.virtualKey(view)
                quoteModel.getAll {
                    val quotes = it.toMutableList()
                    quotes.removeAll(list)
                    AddQuoteSheet().show(this@ListActivity) {
                        displayToolbar(false)
                        displayHandle(true)
                        with(quotes)
                        onPositive { quote ->
                            Feedback.confirm(binding.root)
                            quoteSelected(quote)
                        }
                    }
                }
            }
        }

        setupRecycler((intent.extras ?: return).getInt("id"))
    }

    private fun setupRecycler(id: Int) {
        listModel.get(id) {
            list = it.quotes.toMutableList()
            with(binding) {
                title = it.title
                with(quotesRecycler) {
                    applyInsetter {
                        type(navigationBars = true) {
                            margin(bottom = true)
                        }
                    }
                    layoutManager = LinearLayoutManager(context)
                    adapter = QuoteRecycler(list, this@ListActivity, listId)
                    setHasFixedSize(false)
                }
            }
            checkLength(list)
        }
    }

    private fun checkLength(list: MutableList<Quote>) {
        if (list.isEmpty()) binding.noQuotesText.visibility = View.VISIBLE else View.GONE
    }

    override fun like(quote: Quote) {
        quoteModel.setLike(quote.id, true)
        Snackbar.make(binding.root, "Liked", Snackbar.LENGTH_SHORT).show()
    }

    override fun unlike(quote: Quote) {
        quoteModel.setLike(quote.id, false)
        Snackbar.make(binding.root, "Unliked", Snackbar.LENGTH_SHORT).show()
    }

    override fun bin(quote: Quote) {
        listModel.removeQuote(listId, quote) {
            Snackbar.make(binding.root, "Removed", Snackbar.LENGTH_SHORT).show()
        }
        list.remove(quote)
        checkLength(list)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.list_activity_menu, menu)
        toolbarMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.tune -> showSettings()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun quoteSelected(quote: Quote) {
        listModel.addQuote(listId, quote) {
            setupRecycler(listId)
            checkLength(list)
            binding.noQuotesText.visibility = View.GONE
        }
    }

    private fun showSettings(): Boolean {
        toolbarMenu?.findItem(R.id.tune)?.isEnabled = false
        CustomiseListSheet().show(this, application) {
            displayToolbar(false)
            displayHandle(true)
            displayPositiveButton(false)
            displayNegativeButton(false)
            attachVariables(listModel, listId)
            onClose { toolbarMenu?.findItem(R.id.tune)?.isEnabled = true }
        }
        return true
    }
}
