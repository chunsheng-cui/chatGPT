package com.jeanboy.component.wheelfortune

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cui.utils.LogUtil
import com.reword.adbase.ui.MyDialogBuilder
import com.cui.utils.ToastUtils
import com.reword.adbase.StatisticsContainerConstant
import com.reword.adbase.StatisticsNameConstant
import com.reword.adbase.TaskCenterAdManagerImpl
import com.reword.adbase.coins.TimesCoinsWheelManager
import com.reword.adbase.report
import com.reword.adbase.ui.DialogViewHolder
import com.reword.adbase.ui.dialog.LoadingDialog
import com.reword.adbase.units.BannerUnits
import com.reword.adbase.units.InterUnits
import com.reword.adbase.units.NativeUnits
import com.reword.adbase.units.RewordUnits
import kotlin.random.Random

fun showWheelActivity(context: Context) {
    context.startActivity(Intent(context, WheelActivity::class.java))
}

class WheelActivity : AppCompatActivity() {

    private var isRunning: Boolean = false

    /**
     * 金币数量
     */
    private val dataList = mutableListOf(
        50, 200, 100, 50, 150, 100, 300, 100
    )

    /**
     * 转盘
     */
    private var wheelFortuneView: WheelFortuneView? = null

    /**
     * 开始按钮
     */
    private var tvStart: TextView? = null

    /**
     * 剩余次数
     */
    private var tvLeft: TextView? = null

    /**
     * 金币总数
     */
    private var tvCoins: TextView? = null

    /**
     * 金币总数
     */
    private var mTimesManager: TimesCoinsWheelManager? = null

    /**
     * 加载框
     */
    private val loadingDialog: LoadingDialog by lazy {
        LoadingDialog(this)
    }

    private var adInterWheel = InterUnits.taskCenterCoinWheelInter
    private var adInterTimes = InterUnits.taskCenterCoinWheel1Inter
    private var adReword = RewordUnits.taskCenterCoinWheel5Reward
    private var adNativeWheel = NativeUnits.taskCenterCoinWheelNative

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wheel)
        initView()
        //加载banner广告
        initBannerAd()
        //初始化转盘
        initWheel()
        //初始化次数
        getTimesManager().refreshTimes()
        //刷新次数
        refreshTimes()
        //刷新金币总数
        refreshCoins()
        //获取1次机会
        findViewById<TextView>(R.id.tv_one_more_time).setOnClickListener {
            report(
                StatisticsNameConstant.STATISTICS_NAME_1MORETIME_CLICK,
                StatisticsContainerConstant.STATISTICS_CONTAINER_COINWHEEL
            )
            getOneTime()
        }
        //获取5次机会
        findViewById<TextView>(R.id.tv_five_more_times).setOnClickListener {
            report(
                StatisticsNameConstant.STATISTICS_NAME_5MORETIME_CLICK,
                StatisticsContainerConstant.STATISTICS_CONTAINER_COINWHEEL
            )
            getFiveTimes()
        }
        //开始游戏
        tvStart?.apply {
            setOnClickListener {
                report(
                    StatisticsNameConstant.STATISTICS_NAME_START_CLICK,
                    StatisticsContainerConstant.STATISTICS_CONTAINER_COINWHEEL
                )
                if (isRunning) return@setOnClickListener
                isRunning = true
                //判断剩余次数
                getTimesManager().apply {
                    if (getTimesLeft() <= 0) {
                        ToastUtils.showToast(context, getString(R.string.no_more_times))
                        return@setOnClickListener
                    }
                    //预加载广告
                    TaskCenterAdManagerImpl.apply {
                        preloadInter(adInterWheel)
                        preloadNative(adNativeWheel)
                    }
                    //增加已用次数
                    addTimesUsed()
                }
                //刷新次数
                refreshTimes()
                //开始转盘
                wheelFortuneView?.onJoinClick()
            }
        }
    }

    /**
     * 加载banner广告
     */
    private fun initBannerAd() {
        TaskCenterAdManagerImpl.loadBanner(
            this, findViewById(R.id.ad_banner), BannerUnits.taskCenterCoinWheelBanner
        )
    }

    /**
     * 获取5次机会
     */
    private fun getFiveTimes() {
        //开启加载弹窗
        loadingDialog.show()
        TaskCenterAdManagerImpl.loadToShowReword(adReword, success = {
            if (it) {
                //增加1次
                getTimesManager().addTimesTotal(5)
                //刷新次数
                refreshTimes()
            } else {
                ToastUtils.showToast(this, getString(R.string.get_ad_failed))
            }
            //关闭加载弹窗
            loadingDialog.dismiss()
        })
    }

    /**
     * 获取1次机会,插屏广告
     */
    private fun getOneTime() {
        //开启加载弹窗
        loadingDialog.show()
        TaskCenterAdManagerImpl.loadToShowInter(adInterTimes, success = {
            if (it) {
                //增加1次
                getTimesManager().addTimesTotal(1)
                //刷新次数
                refreshTimes()
            } else {
                ToastUtils.showToast(this, getString(R.string.get_ad_failed))
            }
            //关闭加载弹窗
            loadingDialog.dismiss()
        })
    }

    /**
     * 转盘次数管理类
     */
    private fun getTimesManager() = mTimesManager ?: TimesCoinsWheelManager(this)

    /**
     * 初始化布局
     */
    private fun initView() {
        // 返回按钮
        findViewById<TextView>(R.id.tv_back).setOnClickListener {
            onBackPressed()
        }
        // 开始按钮剩余次数
        tvStart = findViewById(R.id.tv_start)
        tvCoins = findViewById(R.id.tv_coin_wheel_coins)
        tvLeft = findViewById(R.id.tv_times_left_num)
    }

    /**
     * 获取金币总数
     */
    private fun refreshCoins() {
        tvCoins?.text = getTimesManager().getCoins().toString()
    }

    /**
     * 初始化转盘
     */
    private fun initWheel() {
        wheelFortuneView = findViewById(R.id.wheelFortuneView)
        wheelFortuneView?.apply {
            //设置数据
            setData(dataList)
            //设置监听
            setListener(object : WheelFortuneView.WheelStateListener {
                override fun onJoinClick() {
                    wheelFortuneView?.toRunning(Random.nextInt(dataList.size))
                }

                override fun onWheelEnd(selectData: Int) {
                    //转盘弹框
                    showCoinsDialog(selectData)
                }
            })
        }
    }

    /**
     * 金币弹窗
     */
    private fun showCoinsDialog(selectData: Int) {
        report(
            StatisticsNameConstant.STATISTICS_NAME_GETCOIN_CLICK,
            StatisticsContainerConstant.STATISTICS_CONTAINER_COINWHEELALERT
        )
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_reword, null, false)
        val dialog = MyDialogBuilder(this).setView(view, adNativeWheel).create().show()
        DialogViewHolder(view).apply {
            // 描述文案
            tvContentLeft.text = getString(R.string.you_got)
            // 广告角标
            ivAd.visibility = View.VISIBLE
            //金币数量
            tvContent.text = selectData.toString()
            tvClaimNow.setOnClickListener {
                claimNow(selectData, dialog)
            }
            ivClose.setOnClickListener {
                claimNow(selectData, dialog)
            }
        }
        isRunning = false
    }

    /**
     * 领取金币逻辑
     */
    private fun claimNow(selectData: Int, dialog: AlertDialog?) {
        if (!TaskCenterAdManagerImpl.isInterReady(adInterWheel)) {
            LogUtil.d("claimNow AD is not ready:$adInterWheel")
            loadingDialog.show()
        }
        //插屏广告
        TaskCenterAdManagerImpl.loadToShowInter(adInterWheel, success = {
            LogUtil.d("coins wheel claim ad show $it")
            //关闭加载弹窗
            loadingDialog.dismiss()
        })
        //增加金币数
        getTimesManager().addCoins(selectData.toLong())
        //刷新金币数
        refreshCoins()
        //关闭弹窗
        dialog?.dismiss()
    }

    /**
     * 刷新次数
     */
    private fun refreshTimes() {
        tvStart?.text = getString(
            R.string.start, getTimesManager().getTimesUsed(), getTimesManager().getTimesTotal()
        )
        tvLeft?.text = getTimesManager().getTimesLeft().toString()
    }
}