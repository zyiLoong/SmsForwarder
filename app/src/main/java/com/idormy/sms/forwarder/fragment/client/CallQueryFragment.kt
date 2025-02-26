package com.idormy.sms.forwarder.fragment.client

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.vlayout.DelegateAdapter
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.alibaba.android.vlayout.layout.LinearLayoutHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.base.broccoli.BroccoliSimpleDelegateAdapter
import com.idormy.sms.forwarder.adapter.base.delegate.SimpleDelegateAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientCallQueryBinding
import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.server.model.CallQueryData
import com.idormy.sms.forwarder.utils.*
import com.jeremyliao.liveeventbus.LiveEventBus
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageActivity
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.utils.SnackbarUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.searchview.MaterialSearchView
import com.xuexiang.xutil.data.DateUtils
import com.xuexiang.xutil.system.ClipboardUtils
import me.samlss.broccoli.Broccoli

@Suppress("PropertyName")
@Page(name = "远程查通话")
class CallQueryFragment : BaseFragment<FragmentClientCallQueryBinding?>() {

    val TAG: String = CallQueryFragment::class.java.simpleName
    private var mAdapter: SimpleDelegateAdapter<CallInfo>? = null
    private var callType: Int = 3
    private var pageNum: Int = 1
    private val pageSize: Int = 20
    private var keyword: String = ""

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentClientCallQueryBinding {
        return FragmentClientCallQueryBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar.setTitle(R.string.api_call_query)
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_query) {
            @SingleClick
            override fun performAction(view: View) {
                binding!!.searchView.showSearch()
            }
        })
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        val virtualLayoutManager = VirtualLayoutManager(requireContext())
        binding!!.recyclerView.layoutManager = virtualLayoutManager
        val viewPool = RecyclerView.RecycledViewPool()
        binding!!.recyclerView.setRecycledViewPool(viewPool)
        viewPool.setMaxRecycledViews(0, 10)

        mAdapter = object : BroccoliSimpleDelegateAdapter<CallInfo>(
            R.layout.adapter_call_card_view_list_item,
            LinearLayoutHelper(),
            DataProvider.emptyCallInfo
        ) {
            override fun onBindData(
                holder: RecyclerViewHolder,
                model: CallInfo,
                position: Int,
            ) {
                val from = if (TextUtils.isEmpty(model.name)) model.number else model.number + " | " + model.name
                holder.text(R.id.tv_from, from)
                holder.text(R.id.tv_time, DateUtils.getFriendlyTimeSpanByNow(model.dateLong))
                holder.image(R.id.iv_image, model.typeImageId)
                holder.image(R.id.iv_sim_image, model.simImageId)
                holder.text(R.id.tv_duration, ResUtils.getString(R.string.call_duration) + model.duration + ResUtils.getString(R.string.seconds))
                holder.image(R.id.iv_copy, R.drawable.ic_copy)
                holder.image(R.id.iv_call, R.drawable.ic_phone_out)
                holder.image(R.id.iv_reply, R.drawable.ic_reply)
                holder.click(R.id.iv_copy) {
                    XToastUtils.info(String.format(getString(R.string.copied_to_clipboard), from))
                    ClipboardUtils.copyText(from)
                }
                holder.click(R.id.iv_call) {
                    XToastUtils.info(getString(R.string.local_call) + model.number)
                    PhoneUtils.dial(model.number)
                }
                holder.click(R.id.iv_reply) {
                    XToastUtils.info(getString(R.string.remote_sms) + model.number)
                    LiveEventBus.get<Int>(EVENT_KEY_SIM_SLOT).post(model.simId)
                    LiveEventBus.get<String>(EVENT_KEY_PHONE_NUMBERS).post(model.number)
                    PageOption.to(SmsSendFragment::class.java).setNewActivity(true).open((context as XPageActivity?)!!)
                }
            }

            override fun onBindBroccoli(holder: RecyclerViewHolder, broccoli: Broccoli) {
                broccoli.addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_from)))
                    .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_time)))
                    .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.iv_sim_image)))
                    .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_duration)))
                    .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.iv_image)))
                    .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.iv_copy)))
                    .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.iv_call)))
                    .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.iv_reply)))
            }
        }

        val delegateAdapter = DelegateAdapter(virtualLayoutManager)
        delegateAdapter.addAdapter(mAdapter)
        binding!!.recyclerView.adapter = delegateAdapter

        binding!!.tabBar.setTabTitles(ResUtils.getStringArray(R.array.call_type_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            callType = 3 - position
            pageNum = 1
            loadRemoteData(true)
            binding!!.recyclerView.scrollToPosition(0)
        }

        //搜索框
        binding!!.searchView.findViewById<View>(com.xuexiang.xui.R.id.search_layout).visibility = View.GONE
        binding!!.searchView.setVoiceSearch(true)
        binding!!.searchView.setEllipsize(true)
        binding!!.searchView.setSuggestions(resources.getStringArray(R.array.query_suggestions))
        binding!!.searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                SnackbarUtils.Indefinite(view, String.format(getString(R.string.search_keyword), query)).info()
                    .actionColor(ResUtils.getColor(R.color.xui_config_color_white))
                    .setAction(getString(R.string.clear)) {
                        keyword = ""
                        loadRemoteData(true)
                    }.show()
                if (keyword != query) {
                    keyword = query
                    loadRemoteData(true)
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                //Do some magic
                return false
            }
        })
        binding!!.searchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewShown() {
                //Do some magic
            }

            override fun onSearchViewClosed() {
                //Do some magic
            }
        })
        binding!!.searchView.setSubmitOnClick(true)
    }

    override fun initListeners() {
        //下拉刷新
        binding!!.refreshLayout.setOnRefreshListener { refreshLayout: RefreshLayout ->
            refreshLayout.layout.postDelayed({
                pageNum = 1
                loadRemoteData(true)
            }, 1000)
        }
        //上拉加载
        binding!!.refreshLayout.setOnLoadMoreListener { refreshLayout: RefreshLayout ->
            refreshLayout.layout.postDelayed({
                loadRemoteData(false)
            }, 1000)
        }
        binding!!.refreshLayout.autoRefresh() //第一次进入触发自动刷新，演示效果
    }

    private fun loadRemoteData(refresh: Boolean) {

        val requestUrl: String = HttpServerUtils.serverAddress + "/call/query"
        Log.i(TAG, "requestUrl:$requestUrl")

        val msgMap: MutableMap<String, Any> = mutableMapOf()
        val timestamp = System.currentTimeMillis()
        msgMap["timestamp"] = timestamp
        val clientSignKey = HttpServerUtils.clientSignKey
        if (!TextUtils.isEmpty(clientSignKey)) {
            msgMap["sign"] = HttpServerUtils.calcSign(timestamp.toString(), clientSignKey.toString())
        }

        /*val dataMap: MutableMap<String, Any> = mutableMapOf()
        dataMap["type"] = callType
        dataMap["page_num"] = pageNum
        dataMap["page_size"] = pageSize
        msgMap["data"] = dataMap*/
        msgMap["data"] = CallQueryData(callType, pageNum, pageSize, keyword)

        val requestMsg: String = Gson().toJson(msgMap)
        Log.i(TAG, "requestMsg:$requestMsg")

        XHttp.post(requestUrl)
            .upJson(requestMsg)
            .keepJson(true)
            .timeOut((SettingUtils.requestTimeout * 1000).toLong()) //超时时间10s
            .cacheMode(CacheMode.NO_CACHE)
            //.retryCount(SettingUtils.requestRetryTimes) //超时重试的次数
            //.retryDelay(SettingUtils.requestDelayTime) //超时重试的延迟时间
            //.retryIncreaseDelay(SettingUtils.requestDelayTime) //超时重试叠加延时
            .timeStamp(true)
            .execute(object : SimpleCallBack<String>() {

                override fun onError(e: ApiException) {
                    XToastUtils.error(e.displayMessage)
                }

                override fun onSuccess(response: String) {
                    Log.i(TAG, response)
                    try {
                        val resp: BaseResponse<List<CallInfo>?> = Gson().fromJson(response, object : TypeToken<BaseResponse<List<CallInfo>?>>() {}.type)
                        if (resp.code == 200) {
                            //XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
                            pageNum++
                            if (refresh) {
                                mAdapter!!.refresh(resp.data)
                                binding!!.refreshLayout.finishRefresh()
                                binding!!.recyclerView.scrollToPosition(0)
                            } else {
                                mAdapter!!.loadMore(resp.data)
                                binding!!.refreshLayout.finishLoadMore()
                            }
                        } else {
                            XToastUtils.error(ResUtils.getString(R.string.request_failed) + resp.msg)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        XToastUtils.error(ResUtils.getString(R.string.request_failed) + response)
                    }
                }

            })

    }

}