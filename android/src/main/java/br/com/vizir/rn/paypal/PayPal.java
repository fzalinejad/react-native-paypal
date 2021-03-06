package br.com.vizir.rn.paypal;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

public class PayPal extends ReactContextBaseJavaModule {

  private static final String ERROR_USER_CANCELLED = "USER_CANCELLED";
  private static final String ERROR_INVALID_CONFIG = "INVALID_CONFIG";

  private Callback successCallback;
  private Callback errorCallback;
  public static final int INTENT_REQ_CODE = 10011;

  public PayPal(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addActivityEventListener(new PyaPalActivityEventListener());
  }

  private class PyaPalActivityEventListener extends BaseActivityEventListener {
    @Override
    public void onActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent intent) {
      handleActivityResult(requestCode, resultCode, intent);
    }
  }

  @Override
  public String getName() {
    return "PayPal";
  }

  @Override public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();

    constants.put("NO_NETWORK", PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
    constants.put("SANDBOX", PayPalConfiguration.ENVIRONMENT_SANDBOX);
    constants.put("PRODUCTION", PayPalConfiguration.ENVIRONMENT_PRODUCTION);
    constants.put(ERROR_USER_CANCELLED, ERROR_USER_CANCELLED);
    constants.put(ERROR_INVALID_CONFIG, ERROR_INVALID_CONFIG);
    constants.put("PAYMENT_INTENT_SALE", PayPalPayment.PAYMENT_INTENT_SALE);
    constants.put("PAYMENT_INTENT_AUTHORIZE", PayPalPayment.PAYMENT_INTENT_AUTHORIZE);
    constants.put("PAYMENT_INTENT_ORDER", PayPalPayment.PAYMENT_INTENT_ORDER);

    return constants;
  }

  @ReactMethod
  public void paymentRequest(
    final ReadableMap payPalParameters,
    final Callback successCallback,
    final Callback errorCallback
  ) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    final String environment = payPalParameters.getString("environment");
    final String clientId = payPalParameters.getString("clientId");
    final String price = payPalParameters.getString("price");
    final String currency = payPalParameters.getString("currency");
    final String description = payPalParameters.getString("description");
    final String paymentIntent = payPalParameters.hasKey("paymentIntent")
      ? payPalParameters.getString("paymentIntent")
      : PayPalPayment.PAYMENT_INTENT_SALE;
    final String merchantName = payPalParameters.hasKey("merchantName")
      ? payPalParameters.getString("merchantName")
      : null;
    final boolean acceptCreditCards = payPalParameters.hasKey("acceptCreditCards")
      ? payPalParameters.getBoolean("acceptCreditCards")
      : true;
    final String defaultUserEmail = payPalParameters.hasKey("defaultUserEmail")
      ? payPalParameters.getString("defaultUserEmail")
      : null;
    final String softDescriptor = payPalParameters.hasKey("softDescriptor")
      ? payPalParameters.getString("softDescriptor")
      : null;

    PayPalConfiguration config =
      new PayPalConfiguration().environment(environment).clientId(clientId)
      .merchantName(merchantName)
      .acceptCreditCards(acceptCreditCards)
      .defaultUserEmail(defaultUserEmail);

    startPayPalService(config);

    PayPalPayment thingToBuy =
      new PayPalPayment(new BigDecimal(price), currency, description, paymentIntent)
      .softDescriptor(softDescriptor);

    Intent intent =
      new Intent(getCurrentActivity(), PaymentActivity.class)
      .putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config)
      .putExtra(PaymentActivity.EXTRA_PAYMENT, thingToBuy);

    getCurrentActivity().startActivityForResult(intent, INTENT_REQ_CODE);
  }

  private void startPayPalService(PayPalConfiguration config) {
    Intent intent = new Intent(getCurrentActivity(), PayPalService.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    getCurrentActivity().startService(intent);
  }

  public void handleActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode != INTENT_REQ_CODE) { return; }

    if (resultCode == Activity.RESULT_OK) {
      PaymentConfirmation confirm =
        data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
      if (confirm != null) {
        successCallback.invoke(
          confirm.toJSONObject().toString(),
          confirm.getPayment().toJSONObject().toString()
        );
      }
    } else if (resultCode == Activity.RESULT_CANCELED) {
      errorCallback.invoke(ERROR_USER_CANCELLED);
    } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
      errorCallback.invoke(ERROR_INVALID_CONFIG);
    }

    getReactApplicationContext().stopService(new Intent(getReactApplicationContext(), PayPalService.class));
  }
}
