package com.hyperfetch.engine;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 网络状态监控器
 * 负责监听网络变化，在 Wi-Fi 断开时暂停所有任务，Wi-Fi 恢复时恢复任务
 */
public class NetworkStateMonitor {

    private static final String TAG = "NetworkStateMonitor";

    /**
     * 网络类型枚举
     */
    public enum NetworkType {
        /**
         * 无网络连接
         */
        NONE,

        /**
         * Wi-Fi 网络
         */
        WIFI,

        /**
         * 移动网络
         */
        MOBILE,

        /**
         * 以太网
         */
        ETHERNET,

        /**
         * 其他网络类型
         */
        OTHER
    }

    /**
     * 网络状态回调接口
     */
    public interface NetworkStateCallback {
        /**
         * 网络已连接回调
         *
         * @param networkType 网络类型
         */
        void onNetworkConnected(NetworkType networkType);

        /**
         * 网络已断开回调
         */
        void onNetworkDisconnected();

        /**
         * Wi-Fi 已连接回调
         */
        void onWifiConnected();

        /**
         * Wi-Fi 已断开回调
         */
        void onWifiDisconnected();

        /**
         * 切换到移动网络回调
         */
        void onMobileNetworkConnected();
    }

    /**
     * 应用上下文
     */
    private final Context context;

    /**
     * 连接管理器
     */
    private final ConnectivityManager connectivityManager;

    /**
     * 网络回调
     */
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * 网络状态回调列表
     */
    private final CopyOnWriteArrayList<NetworkStateCallback> callbacks;

    /**
     * 当前网络类型
     */
    private volatile NetworkType currentNetworkType;

    /**
     * 是否已注册网络回调
     */
    private volatile boolean isRegistered;

    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public NetworkStateMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        this.callbacks = new CopyOnWriteArrayList<>();
        this.currentNetworkType = NetworkType.NONE;
        this.isRegistered = false;
    }

    /**
     * 注册网络状态监听
     */
    public void register() {
        if (isRegistered) {
            return;
        }

        // 构建网络请求
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        // 创建网络回调
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                NetworkType networkType = getNetworkType(network);
                currentNetworkType = networkType;
                notifyNetworkConnected(networkType);

                if (networkType == NetworkType.WIFI) {
                    notifyWifiConnected();
                } else if (networkType == NetworkType.MOBILE) {
                    notifyMobileNetworkConnected();
                }
            }

            @Override
            public void onLost(Network network) {
                NetworkType previousType = currentNetworkType;
                currentNetworkType = NetworkType.NONE;
                notifyNetworkDisconnected();

                if (previousType == NetworkType.WIFI) {
                    notifyWifiDisconnected();
                }
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                NetworkType networkType = getNetworkType(network, capabilities);
                if (networkType != currentNetworkType) {
                    NetworkType previousType = currentNetworkType;
                    currentNetworkType = networkType;

                    if (networkType == NetworkType.WIFI && previousType != NetworkType.WIFI) {
                        notifyWifiConnected();
                    } else if (networkType == NetworkType.MOBILE && previousType == NetworkType.WIFI) {
                        notifyWifiDisconnected();
                        notifyMobileNetworkConnected();
                    }
                }
            }
        };

        // 注册网络回调
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } else {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }

        // 获取当前网络状态
        NetworkType initialType = getCurrentNetworkType();
        currentNetworkType = initialType;

        isRegistered = true;
    }

    /**
     * 注销网络状态监听
     */
    public void unregister() {
        if (!isRegistered || networkCallback == null) {
            return;
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception e) {
            // 忽略注销异常
        }

        isRegistered = false;
        networkCallback = null;
    }

    /**
     * 获取当前网络类型
     *
     * @return 当前网络类型
     */
    public NetworkType getCurrentNetworkType() {
        if (connectivityManager == null) {
            return NetworkType.NONE;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return NetworkType.NONE;
            }

            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(network);
            if (capabilities == null) {
                return NetworkType.NONE;
            }

            return getNetworkType(network, capabilities);
        } else {
            // 兼容旧版本
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                return NetworkType.NONE;
            }

            switch (networkInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return NetworkType.WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                    return NetworkType.MOBILE;
                case ConnectivityManager.TYPE_ETHERNET:
                    return NetworkType.ETHERNET;
                default:
                    return NetworkType.OTHER;
            }
        }
    }

    /**
     * 获取网络类型
     *
     * @param network 网络对象
     * @return 网络类型
     */
    private NetworkType getNetworkType(Network network) {
        if (connectivityManager == null) {
            return NetworkType.NONE;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return NetworkType.NONE;
        }

        return getNetworkType(network, capabilities);
    }

    /**
     * 获取网络类型
     *
     * @param network      网络对象
     * @param capabilities 网络能力
     * @return 网络类型
     */
    private NetworkType getNetworkType(Network network, NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkType.WIFI;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkType.MOBILE;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return NetworkType.ETHERNET;
        } else {
            return NetworkType.OTHER;
        }
    }

    /**
     * 检查网络是否可用
     *
     * @return 网络是否可用
     */
    public boolean isNetworkAvailable() {
        return currentNetworkType != NetworkType.NONE;
    }

    /**
     * 检查是否为 Wi-Fi 网络
     *
     * @return 是否为 Wi-Fi 网络
     */
    public boolean isWifiConnected() {
        return currentNetworkType == NetworkType.WIFI;
    }

    /**
     * 检查是否为移动网络
     *
     * @return 是否为移动网络
     */
    public boolean isMobileNetwork() {
        return currentNetworkType == NetworkType.MOBILE;
    }

    /**
     * 添加网络状态回调
     *
     * @param callback 网络状态回调
     */
    public void addCallback(NetworkStateCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    /**
     * 移除网络状态回调
     *
     * @param callback 网络状态回调
     */
    public void removeCallback(NetworkStateCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * 清除所有回调
     */
    public void clearCallbacks() {
        callbacks.clear();
    }

    /**
     * 通知网络已连接
     *
     * @param networkType 网络类型
     */
    private void notifyNetworkConnected(NetworkType networkType) {
        for (NetworkStateCallback callback : callbacks) {
            try {
                callback.onNetworkConnected(networkType);
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知网络已断开
     */
    private void notifyNetworkDisconnected() {
        for (NetworkStateCallback callback : callbacks) {
            try {
                callback.onNetworkDisconnected();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知 Wi-Fi 已连接
     */
    private void notifyWifiConnected() {
        for (NetworkStateCallback callback : callbacks) {
            try {
                callback.onWifiConnected();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知 Wi-Fi 已断开
     */
    private void notifyWifiDisconnected() {
        for (NetworkStateCallback callback : callbacks) {
            try {
                callback.onWifiDisconnected();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知切换到移动网络
     */
    private void notifyMobileNetworkConnected() {
        for (NetworkStateCallback callback : callbacks) {
            try {
                callback.onMobileNetworkConnected();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 检查是否已注册
     *
     * @return 是否已注册
     */
    public boolean isRegistered() {
        return isRegistered;
    }
}