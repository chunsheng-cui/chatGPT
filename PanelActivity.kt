package com.reword.panel

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cui.utils.LogUtil
import com.reword.adbase.ui.MyDialogBuilder
import com.cui.utils.ToastUtils
import com.example.panel.R
import com.reword.adbase.StatisticsContainerConstant
import com.reword.adbase.StatisticsNameConstant
import com.reword.adbase.TaskCenterAdManagerImpl
import com.reword.adbase.coins.TimesLuckyWheelManager
import com.reword.adbase.report
import com.reword.adbase.ui.DialogViewHolder
import com.reword.adbase.ui.dialog.LoadingDialog
import com.reword.adbase.units.BannerUnits
import com.reword.adbase.units.InterUnits
import com.reword.adbase.units.NativeUnits
import com.reword.adbase.units.RewordUnits
import java.util.*

fun showPanelActivity(context: Context) {
    context.startActivity(Intent(context, PanelActivity::class.java))
}

class PanelActivity : AppCompatActivity() {
    // 声明一个 Handler
    private val handler = Handler(Looper.getMainLooper())
    private var luckyPanel: LuckyMonkeyPanelView? = null

    private val rewards = arrayOf(50, 100, 150, 200, 300, 500)
    private val probabilities = arrayOf(0.25, 0.35, 0.15, 0.1, 0.1, 0.05)


    /**
     * 开始按钮
     */
    private var tvStart: TextView? = null

    /**
     * 金币总数
     */
    private var tvCoins: TextView? = null

    /**
     * 剩余次数
     */
    private var tvLeft: TextView? = null

    /**
     * 金币总数
     */
    private var mTimesManager: TimesLuckyWheelManager? = null

    /**
     * 加载框
     */
    private val loadingDialog: LoadingDialog by lazy {
        LoadingDialog(this)
    }
    private var adInterWheel = InterUnits.taskCenterLuckyWheelInter
    private var adInterTimes = InterUnits.taskCenterLuckyWheel1Inter
    private var adReword = RewordUnits.taskCenterLuckyWheel5Reward
    private var adNativeWheel = NativeUnits.taskCenterLuckyWheelNative

    private var isRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel)
        initView()
        //加载banner广告
        initBannerAd()
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
                StatisticsContainerConstant.STATISTICS_CONTAINER_LUCKYWHEEL
            )
            getOneTime()
        }
        //获取5次机会
        findViewById<TextView>(R.id.tv_five_more_times).setOnClickListener {
            report(
                StatisticsNameConstant.STATISTICS_NAME_5MORETIME_CLICK,
                StatisticsContainerConstant.STATISTICS_CONTAINER_LUCKYWHEEL
            )
            getFiveTimes()
        }
        //开始游戏
        tvStart?.apply {
            setOnClickListener {
                report(
                    StatisticsNameConstant.STATISTICS_NAME_START_CLICK,
                    StatisticsContainerConstant.STATISTICS_CONTAINER_LUCKYWHEEL
                )
                if (isRunning) return@setOnClickListener
                isRunning = true
                //判断剩余次数
                getTimesManager().apply {
                    if (timesLeft() <= 0) {
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
                //开始游戏
                luckyPanel?.startGame()
                handler.postDelayed({
                    stopPanel()
                }, 4000L)
            }
        }
    }

    /**
     * 加载banner广告
     */
    private fun initBannerAd() {
        TaskCenterAdManagerImpl.loadBanner(
            this, findViewById(R.id.ad_banner), BannerUnits.taskCenterLuckyWheelBanner
        )
    }

    /**
     * 初始化布局
     */
    private fun initView() {
        // 返回按钮
        findViewById<TextView>(R.id.tv_back).setOnClickListener {
            onBackPressed()
        }
        luckyPanel = findViewById(R.id.lucky_panel)
        // 开始按钮剩余次数
        tvStart = findViewById(R.id.tv_start)
        tvCoins = findViewById(R.id.tv_lucky_wheel_coins)
        tvLeft = findViewById(R.id.tv_times_left_num)
    }

    /**
     * 停止转盘
     */
    private fun stopPanel() {
        luckyPanel?.tryToStop(Random().nextInt(8))
        luckyPanel?.setListener(object : LuckyPanelListener {
            override fun onSelected(index: Int) {
                showCoinsDialog(getRandomReward())
            }
        })
    }

    /**
     * 金币弹窗
     */
    private fun showCoinsDialog(selectData: Int) {
        report(
            StatisticsNameConstant.STATISTICS_NAME_GETCOIN_CLICK,
            StatisticsContainerConstant.STATISTICS_CONTAINER_LUCKYWHEELALERT
        )
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_reword, null, false)
        val dialog =
            MyDialogBuilder(this).setView(view, adNativeWheel).create()
                .show()
        DialogViewHolder(view).apply {
            // 描述文案
            tvContentLeft.text = getString(R.string.you_got)
            // 金币数量
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
            //开启加载弹窗
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
     * 获取5次机会
     */
    private fun getFiveTimes() {
        //开启加载弹窗
        loadingDialog.show()
        //预加载广告
        TaskCenterAdManagerImpl.loadToShowReword(adReword, success = {
            if (it) {
                //增加5次
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
        //直接加载广告展示
        TaskCenterAdManagerImpl.loadToShowInter(adInterTimes, success = {
            if (it) {
                //增加1次
                getTimesManager().addTimesTotal(1)
                //刷新次数
                refreshTimes()
            } else {
                ToastUtils.showToast(this, getString(R.string.get_ad_failed))
            }
            //关闭弹窗
            loadingDialog.dismiss()
        })
    }

    /**
     * 获取金币总数
     */
    private fun refreshCoins() {
        tvCoins?.text = getTimesManager().getCoins().toString()
    }

    /**
     * 转盘次数管理类
     */
    private fun getTimesManager() = mTimesManager ?: TimesLuckyWheelManager(this)

    override fun onDestroy() {
        super.onDestroy()
        // 移除 Handler 的所有消息和回调，避免内存泄漏
        handler.removeCallbacksAndMessages(null)
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


    /**
     * 随机金币数
     */
    fun getRandomReward(): Int {
        val random = Math.random()
        var total = 0.0
        for (i in probabilities.indices) {
            total += probabilities[i]
            if (random < total) {
                return rewards[i]
            }
        }
        return rewards.last()
    }
}