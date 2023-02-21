package com.reword.tigerwheel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cui.utils.MyDateUtils;
import com.reword.adbase.ui.MyDialogBuilder;
import com.cui.utils.ToastUtils;
import com.example.tigerwheel.BuildConfig;
import com.example.tigerwheel.R;
import com.reword.adbase.StatisticsContainerConstant;
import com.reword.adbase.StatisticsHelpKt;
import com.reword.adbase.StatisticsNameConstant;
import com.reword.adbase.TaskCenterAdManagerImpl;
import com.reword.adbase.coins.CoinsManager;
import com.reword.adbase.coins.TimesFruitLotteryManager;
import com.reword.adbase.ui.DialogViewHolder;
import com.reword.adbase.units.BannerUnits;
import com.reword.adbase.units.InterUnits;
import com.reword.adbase.units.NativeUnits;
import com.reword.adbase.units.RewordUnits;
import com.reword.tigerwheel.widget.OnWheelScrollListener;
import com.reword.tigerwheel.widget.WheelView;
import com.reword.tigerwheel.widget.adapters.AbstractWheelAdapter;

import java.util.Random;

public class TigerWheelActivity extends AppCompatActivity {

    private final String TAG = TigerWheelActivity.class.getSimpleName();
    private boolean DEBUG = BuildConfig.DEBUG;
    private TextView timeLeftView;
    private CoinsManager coinsManager;
    private int lastOnePrizeIndex = 0;
    private int lastTwoPrizeIndex = 0;
    private int lastThreePrizeIndex = 0;
    private Button startView;
    private int prizeLevel = 0;
    private TimesFruitLotteryManager timesFruitLotteryManager;
    private WheelView wheelOneView;
    private WheelView wheelTwoView;
    private WheelView wheelThreeView;
    private TextView mCoinView;
    /**
     * 车轮滚动的监听器
     */
    private final OnWheelScrollListener scrolledListener = new OnWheelScrollListener() {
        public void onScrollingStarted(WheelView wheel) {
        }

        public void onScrollingFinished(WheelView wheel) {
            if (DEBUG) {
                Log.d(TAG, "滑动停止-prizeLevel=" + prizeLevel);
            }

            if (prizeLevel <= 0) {
                ToastUtils.Companion.showToast(wheel.getContext(), getString(R.string.good_luck), Toast.LENGTH_SHORT);
                return;
            }

            //展示领取金币弹框
            StatisticsHelpKt.report(StatisticsNameConstant.STATISTICS_NAME_5MORETIME_CLICK, StatisticsContainerConstant.STATISTICS_CONTAINER_LOTTERYALERT);
            View view = LayoutInflater.from(wheel.getContext()).inflate(R.layout.dialog_reword, null, false);
            DialogViewHolder holder = new DialogViewHolder(view);
            AlertDialog show = new MyDialogBuilder((Activity) wheel.getContext()).setView(view, NativeUnits.INSTANCE.getTaskCenterFruitLotteryNative()).create().show();

            // 金币数量
            int coins;
            if (prizeLevel == 1) {
                coins = 300;
            } else if (prizeLevel == 2) {
                coins = 150;
            } else {
                coins = 0;
            }
            // 描述文案
            holder.getTvContentLeft().setText(getString(R.string.you_got));
            holder.getTvContent().setText(String.valueOf(coins));
            //根据奖品增加金币 并且展示插屏广告
            holder.getIvClose().setOnClickListener(v -> {
                if (isFastDoubleClick()) {
                    return;
                }
                coinsManager.addCoins(coins);
                uploadCoin();
                TaskCenterAdManagerImpl.INSTANCE.directShowInter(InterUnits.INSTANCE.getTaskCenterFruitLotteryInter(), aBoolean -> null);
                if (show != null) {
                    show.dismiss();
                }
            });
            holder.getTvClaimNow().setOnClickListener(v -> {
                if (isFastDoubleClick()) {
                    return;
                }
                coinsManager.addCoins(coins);
                uploadCoin();
                TaskCenterAdManagerImpl.INSTANCE.directShowInter(InterUnits.INSTANCE.getTaskCenterFruitLotteryInter(), aBoolean -> null);
                if (show != null) {
                    show.dismiss();
                }
            });
        }
    };

    /**
     * 加载banner广告
     */
    private void initBannerAd() {
        TaskCenterAdManagerImpl.INSTANCE.loadBanner(this, findViewById(R.id.ad_banner), BannerUnits.INSTANCE.getTaskCenterFruitLotteryBanner());
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TigerWheelActivity.class);
        if (context instanceof Application) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiger_wheel);
        timesFruitLotteryManager = new TimesFruitLotteryManager(getApplicationContext());
        coinsManager = new CoinsManager(getApplicationContext());
        initData();
        initView();
    }

    private void initData() {
        if (!MyDateUtils.INSTANCE.isToday(timesFruitLotteryManager.getUseTime())) {
            int timesLeft = timesFruitLotteryManager.timesLeft();
            timesFruitLotteryManager.setTimesUsed(0);
            timesFruitLotteryManager.setTimesTotal(Math.max(timesLeft, 10));
        }
    }

    private void initView() {
        //加载banner广告
        initBannerAd();
        findViewById(R.id.tiger_wheel_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mCoinView = findViewById(R.id.tiger_wheel_coin);
        uploadCoin();

        View loadingView = findViewById(R.id.tiger_wheel_loading);
        wheelOneView = findViewById(R.id.tiger_wheel_one);
        initWheel(wheelOneView);
        wheelTwoView = findViewById(R.id.tiger_wheel_two);
        initWheel(wheelTwoView);
        wheelThreeView = findViewById(R.id.tiger_wheel_three);
        initWheel(wheelThreeView);
        startView = findViewById(R.id.tiger_wheel_start);
        startView.setOnClickListener(view -> {
            if (isFastDoubleClick()) {
                return;
            }
            StatisticsHelpKt.report(StatisticsNameConstant.STATISTICS_NAME_START_CLICK, StatisticsContainerConstant.STATISTICS_CONTAINER_LOTTERY);
            if (timesFruitLotteryManager.timesLeft() <= 0) {
                ToastUtils.Companion.showToast(getApplicationContext(), "没有次数", Toast.LENGTH_SHORT);
                return;
            }
            startScroll();
            TaskCenterAdManagerImpl.INSTANCE.preloadInter(InterUnits.INSTANCE.getTaskCenterFruitLotteryInter());
            TaskCenterAdManagerImpl.INSTANCE.preloadNative(NativeUnits.INSTANCE.getTaskCenterFruitLotteryNative());
            timesFruitLotteryManager.saveUseTime();
            timesFruitLotteryManager.addTimesUsed();
            updateCount();
        });
        timeLeftView = findViewById(R.id.tiger_wheel_times);
        findViewById(R.id.tiger_wheel_count_one).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFastDoubleClick()) {
                    return;
                }
                StatisticsHelpKt.report(StatisticsNameConstant.STATISTICS_NAME_1MORETIME_CLICK, StatisticsContainerConstant.STATISTICS_CONTAINER_LOTTERY);
                loadingView.setVisibility(View.VISIBLE);
                TaskCenterAdManagerImpl.INSTANCE.loadToShowInter(InterUnits.INSTANCE.getTaskCenterFruitLottery1Inter(), aBoolean -> {
                    if (aBoolean) {
                        timesFruitLotteryManager.addTimesTotal(1);
                        updateCount();
                    }
                    loadingView.setVisibility(View.GONE);
                    return null;
                });
            }
        });
        findViewById(R.id.tiger_wheel_count_two).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFastDoubleClick()) {
                    return;
                }
                StatisticsHelpKt.report(StatisticsNameConstant.STATISTICS_NAME_5MORETIME_CLICK, StatisticsContainerConstant.STATISTICS_CONTAINER_LOTTERY);
                loadingView.setVisibility(View.VISIBLE);
                TaskCenterAdManagerImpl.INSTANCE.loadToShowReword(RewordUnits.INSTANCE.getTaskCenterFruitLottery5Reward(), aBoolean -> {
                    if (aBoolean) {
                        timesFruitLotteryManager.addTimesTotal(5);
                        updateCount();
                    }
                    loadingView.setVisibility(View.GONE);
                    return null;
                });
            }
        });
        updateCount();
    }

    private void uploadCoin() {
        mCoinView.setText(String.valueOf(coinsManager.getCoins()));
    }

    private void updateCount() {
        startView.setText(getString(R.string.start, timesFruitLotteryManager.getTimesUsed(), timesFruitLotteryManager.getTimesTotal()));
        timeLeftView.setText(String.valueOf(timesFruitLotteryManager.timesLeft()));
    }

    /**
     * 初始化轮子
     */
    private void initWheel(WheelView wheel) {
        wheel.setViewAdapter(new SlotMachineAdapter());
        wheel.setVisibleItems(1);
        if (wheel.getId() == R.id.tiger_wheel_three) {
            wheel.addScrollingListener(scrolledListener);
        }
        wheel.setCyclic(true);
        wheel.setEnabled(false);
        wheel.setDrawShadows(false);
    }

    public void startScroll() {

        int prizeSize = 10;
        int onePrizeIndex = 0;
        int twoPrizeIndex = 0;
        int threePrizeIndex = 0;

        //确定随机概率
        Random random = new Random();
        int prizeCountProbability = random.nextInt(99);

        if (prizeCountProbability < 15) {
            //三种相同几率
            prizeLevel = 1;
            onePrizeIndex = twoPrizeIndex = threePrizeIndex = getRandomPrize(random, prizeSize);
            if (DEBUG) {
                Log.d(TAG, "三种相同几率-onePrizeIndex=" + onePrizeIndex + "  twoPrizeIndex=" + twoPrizeIndex + "  threePrizeIndex=" + threePrizeIndex);
            }
        } else if (prizeCountProbability > 15 && prizeCountProbability < 50) {
            //两种相同几率 ,有三种展示形式  110，101，011
            prizeLevel = 2;
            switch (random.nextInt(3)) {
                case 0:
                    onePrizeIndex = twoPrizeIndex = getRandomPrize(random, prizeSize);
                    while (true) {
                        threePrizeIndex = getRandomPrize(random, prizeSize);
                        if (threePrizeIndex != onePrizeIndex) {
                            break;
                        }
                    }
                    break;
                case 1:
                    onePrizeIndex = threePrizeIndex = getRandomPrize(random, prizeSize);
                    while (true) {
                        twoPrizeIndex = getRandomPrize(random, prizeSize);
                        if (twoPrizeIndex != onePrizeIndex) {
                            break;
                        }
                    }

                    break;
                case 2:
                    twoPrizeIndex = threePrizeIndex = getRandomPrize(random, prizeSize);
                    while (true) {
                        onePrizeIndex = getRandomPrize(random, prizeSize);
                        if (onePrizeIndex != twoPrizeIndex) {
                            break;
                        }
                    }
                    break;
            }
            if (DEBUG) {
                Log.d(TAG, "两种相同几率-onePrizeIndex=" + onePrizeIndex + "  twoPrizeIndex=" + twoPrizeIndex + "  threePrizeIndex=" + threePrizeIndex);
            }
        } else {
            //没有中奖 确保三个随机不一样
            prizeLevel = 0;
            onePrizeIndex = getRandomPrize(random, prizeSize);
            while (true) {
                twoPrizeIndex = getRandomPrize(random, prizeSize);
                if (twoPrizeIndex != onePrizeIndex) {
                    break;
                }
            }
            while (true) {
                threePrizeIndex = getRandomPrize(random, prizeSize);
                if (threePrizeIndex != onePrizeIndex && threePrizeIndex != twoPrizeIndex) {
                    break;
                }
            }
            if (DEBUG) {
                Log.e(TAG, "没有中奖-onePrizeIndex=" + onePrizeIndex + "  twoPrizeIndex=" + twoPrizeIndex + "  threePrizeIndex=" + threePrizeIndex);
            }
        }

        int firstNum = (prizeSize - (lastOnePrizeIndex > 0 ? lastOnePrizeIndex : prizeSize)) + onePrizeIndex + 50;
        int secondNum = (prizeSize - (lastTwoPrizeIndex > 0 ? lastTwoPrizeIndex : prizeSize)) + twoPrizeIndex + 70;
        int thirdNum = (prizeSize - (lastThreePrizeIndex > 0 ? lastThreePrizeIndex : prizeSize)) + threePrizeIndex + 90;

        if (DEBUG) {
            Log.d(TAG, "奖品位置-firstNum=" + firstNum + "  secondNum=" + secondNum + "  thirdNum=" + thirdNum);
        }

        lastOnePrizeIndex = onePrizeIndex;
        lastTwoPrizeIndex = twoPrizeIndex;
        lastThreePrizeIndex = threePrizeIndex;

        wheelOneView.scroll(firstNum, 2000);
        wheelTwoView.scroll(secondNum, 3000);
        wheelThreeView.scroll(thirdNum, 5000);
    }

    private int getRandomPrize(Random random, int ss) {
        return random.nextInt(ss);
    }

    /**
     * 老虎机适配器
     */
    private class SlotMachineAdapter extends AbstractWheelAdapter {
        @Override
        public int getItemsCount() {
            return 10;
        }

        @Override
        public View getItem(int index, View cachedView, ViewGroup parent) {
            View view;
            if (cachedView != null) {
                view = cachedView;
            } else {
                view = View.inflate(TigerWheelActivity.this, R.layout.item_dialog_tiger_img, null);
            }
            ImageView img = (ImageView) view.findViewById(R.id.iv_dialog_home_tiger);
            switch (index) {
                case 0:
                    img.setImageResource(R.drawable.tiger_wheel_banana);
                    break;

                case 1:
                    img.setImageResource(R.drawable.tiger_wheel_kiwi_fruit);
                    break;

                case 2:
                    img.setImageResource(R.drawable.tiger_wheel_orange);
                    break;

                case 3:
                    img.setImageResource(R.drawable.tiger_wheel_pear);
                    break;

                case 4:
                    img.setImageResource(R.drawable.tiger_wheel_strawberry);
                    break;

                case 5:
                    img.setImageResource(R.drawable.tiger_wheel_watermelon);
                    break;
                case 6:
                    img.setImageResource(R.drawable.tiger_wheel_apple);
                    break;
                case 7:
                    img.setImageResource(R.drawable.tiger_wheel_cherry);
                    break;
                case 8:
                    img.setImageResource(R.drawable.tiger_wheel_mango);
                    break;
                case 9:
                    img.setImageResource(R.drawable.tiger_wheel_grape);
                    break;

            }

            return view;
        }
    }

    private static long lastClickTime;

    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (0 < timeD && timeD < 800) {
            return true;
        }
        lastClickTime = time;
        return false;
    }
}
