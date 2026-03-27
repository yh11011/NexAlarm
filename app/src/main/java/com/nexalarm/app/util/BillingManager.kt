package com.nexalarm.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.nexalarm.app.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google Play Billing 管理器
 *
 * 使用方式：
 * 1. 在 Google Play Console 建立「一次性購買」商品，ID = PREMIUM_PRODUCT_ID
 * 2. 在 AccountScreen 呼叫 BillingManager.launchPurchaseFlow(activity)
 * 3. 監聽 isPremium StateFlow 更新 UI
 *
 * 注意：測試時需使用已加入 Google Play 測試者名單的 Google 帳號
 */
class BillingManager(private val context: Context) {

    companion object {
        private const val TAG = "BillingManager"

        /** 在 Google Play Console 建立商品後，將此 ID 替換為實際的商品 ID */
        const val PREMIUM_PRODUCT_ID = "nexalarm_premium"
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    /** true = Google Play 有確認有效購買（非優惠碼）；用來防止本地直接停用付費版 */
    private val _hasPlayStorePurchase = MutableStateFlow(false)
    val hasPlayStorePurchase: StateFlow<Boolean> = _hasPlayStorePurchase

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { processPurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "使用者取消購買")
            }
            else -> {
                Log.e(TAG, "購買失敗：${billingResult.debugMessage}")
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        // 啟動時從本地快取恢復 premium 狀態
        _isPremium.value = SettingsManager(context).isPremium
        connectToGooglePlay()
    }

    /** 連線 Google Play Billing 服務 */
    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing 連線成功，查詢現有購買記錄")
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing 連線失敗：${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing 服務斷線，將在下次操作時重連")
            }
        })
    }

    /** 啟動 Google Play 購買流程 */
    fun launchPurchaseFlow(activity: Activity) {
        if (!billingClient.isReady) {
            Log.w(TAG, "Billing 尚未準備好，正在重新連線")
            connectToGooglePlay()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PREMIUM_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val (billingResult, productDetailsList) = billingClient.queryProductDetails(params)

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK ||
                productDetailsList.isNullOrEmpty()
            ) {
                Log.e(TAG, "查詢商品失敗：${billingResult.debugMessage}")
                return@launch
            }

            val productDetails = productDetailsList.first()
            val offerToken = productDetails.oneTimePurchaseOfferDetails?.zza() ?: ""

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            withContext(Dispatchers.Main) {
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    /** 查詢使用者現有的已完成購買（應用啟動時呼叫） */
    private fun queryExistingPurchases() {
        CoroutineScope(Dispatchers.IO).launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            val (billingResult, purchasesList) = billingClient.queryPurchasesAsync(params)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchasesList.any { purchase ->
                    purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _hasPlayStorePurchase.value = hasPremium
                setPremiumStatus(hasPremium)
                Log.d(TAG, "查詢現有購買：isPremium=$hasPremium")
            }
        }
    }

    /** 處理購買結果 */
    private fun processPurchase(purchase: Purchase) {
        if (purchase.products.contains(PREMIUM_PRODUCT_ID) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        ) {
            // 確認購買（消耗型商品不需呼叫此步驟，一次性授權型需確認）
            if (!purchase.isAcknowledged) {
                CoroutineScope(Dispatchers.IO).launch {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val result = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        setPremiumStatus(true)
                        Log.d(TAG, "Premium 購買確認成功")
                    }
                }
            } else {
                setPremiumStatus(true)
            }
        }
    }

    private fun setPremiumStatus(isPremium: Boolean) {
        _isPremium.value = isPremium
        FeatureFlags.isPremium = isPremium
        SettingsManager(context).isPremium = isPremium
    }

    fun release() {
        billingClient.endConnection()
    }
}
