package com.mi.mvi.features.main.blog

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.mi.mvi.R
import com.mi.mvi.domain.Constants.Companion.DELETE
import com.mi.mvi.domain.datastate.*
import com.mi.mvi.domain.model.BlogPostView
import com.mi.mvi.events.BlogEventState
import com.mi.mvi.features.main.blog.viewmodel.*
import kotlinx.android.synthetic.main.fragment_view_blog.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class ViewBlogFragment : BaseBlogFragment(R.layout.fragment_view_blog) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        subscribeObservers()
        checkIsAuthor()

        delete_button.setOnClickListener { deleteBlogPost() }
    }

    private fun deleteBlogPost() {
        uiCommunicationListener?.onUIMessageReceived(StateMessage(
            getString(R.string.are_you_sure_delete),
            UIComponentType.AreYouSureDialog(
                object : AreYouSureCallBack {
                    override fun proceed() {
                        viewModel.setEventState(BlogEventState.DeleteBlogPostEvent)
                    }

                    override fun cancel() {
                    }
                }
            ),
            MessageType.INFO
        ))
    }

    private fun checkIsAuthor() {
        viewModel.setEventState(BlogEventState.CheckAuthorBlogPostEvent)
    }

    private fun subscribeObservers() {
        viewModel.dataState.observe(viewLifecycleOwner, Observer { dataState ->
            when (dataState) {
                is DataState.SUCCESS -> {
                    dataStateChangeListener?.onDataStateChangeListener(dataState = dataState)
                    dataState.data?.let { viewState ->
                        viewModel.setIsAuthorOfBlogPost(
                            viewState.viewBlogFields.isAuthor ?: false
                        )
                    }
                }
                is DataState.ERROR -> {
                    dataState.stateMessage?.let { stateMessage ->
                        if (stateMessage.message == DELETE) {
                            viewModel.removeDeletedBlogPost()
                            findNavController().popBackStack()
                        } else {
                            dataStateChangeListener?.onDataStateChangeListener(dataState = dataState)
                        }
                    }
                }
                is DataState.LOADING -> {
                    dataStateChangeListener?.onDataStateChangeListener(dataState = dataState)
                }
            }
        })

        viewModel.viewState.observe(viewLifecycleOwner, Observer { blogViewState ->
            blogViewState?.viewBlogFields?.blogPostEntity?.let { blogPost ->
                setBlogProperties(blogPostMapper.mapToView(blogPost))
            }

            if (blogViewState.viewBlogFields.isAuthor == true) {
                adaptViewToAuthorMode()
            }
        })
    }

    private fun adaptViewToAuthorMode() {
        activity?.invalidateOptionsMenu()
        delete_button.visibility = View.VISIBLE
    }

    private fun setBlogProperties(blogPostEntity: BlogPostView) {
        Glide.with(this)
            .load(blogPostEntity.image)
            .into(blog_image)

        blog_title.text = blogPostEntity.title
        blog_author.text = blogPostEntity.username
        blog_update_date.text = blogPostEntity.date_updated
        blog_body.text = blogPostEntity.body
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (viewModel.isAuthorOfBlogPost()) {
            inflater.inflate(R.menu.edit_view_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (viewModel.isAuthorOfBlogPost()) {
            when (item.itemId) {
                R.id.edit -> {
                    navUpdateBlogFragment()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navUpdateBlogFragment() {
        viewModel.setUpdatedTitle(viewModel.getBlogPost().title!!)
        viewModel.setUpdatedBody(viewModel.getBlogPost().body!!)
        viewModel.setUpdatedUri(viewModel.getBlogPost().image?.toUri()!!)
        findNavController().navigate(R.id.action_viewBlogFragment_to_updateBlogFragment)
    }
}
