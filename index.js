'use strict';
let {NativeModules, Platform} = require('react-native')
let {PayPal, MFLReactNativePayPal} = NativeModules;

let constants;

if (Platform.OS === 'android') {
  constants = {};
  let constantNames = Object.keys(PayPal).filter(p => p == p.toUpperCase());
  constantNames.forEach(c => constants[c] = PayPal[c]);
} else {
  constants = {
    SANDBOX: 0,
    PRODUCTION: 1,
    NO_NETWORK: 2,

    USER_CANCELLED: 'USER_CANCELLED',
    INVALID_CONFIG: 'INVALID_CONFIG',
  }

  Object.keys(MFLReactNativePayPal.PaymentIntent)
    .forEach((key) => {
      constants[`PAYMENT_INTENT_${key.toUpperCase()}`] = MFLReactNativePayPal.PaymentIntent[key];
    })
}

let functions = {
  paymentRequest(payPalParameters) {
    return new Promise(function(resolve, reject) {
      if (Platform.OS === 'android') {
        PayPal.paymentRequest(payPalParameters, (confirm, payment) => {
          resolve(JSON.parse(confirm), JSON.parse(payment))
        }, reject);
      } else {
        MFLReactNativePayPal.initializePaypalEnvironment(payPalParameters.environment, payPalParameters.clientId);
        MFLReactNativePayPal.preparePaymentOfAmount(
          payPalParameters.price,
          payPalParameters.currency,
          payPalParameters.description,
          payPalParameters.paymentIntent || MFLReactNativePayPal.PaymentIntent.Sale
        );
        MFLReactNativePayPal.prepareConfigurationForMerchant(
          payPalParameters.merchantName,
          payPalParameters.acceptCreditCards !== null && payPalParameters.acceptCreditCards !== undefined
            ? payPalParameters.acceptCreditCards
            : true,
          payPalParameters.defaultUserEmail,
        );
        MFLReactNativePayPal.presentPaymentViewControllerForPreparedPurchase((error, payload) => {
          if (error) {
             reject(constants.INVALID_CONFIG, error)
           } else {
            if (payload.status === 1) {
              resolve(payload.confirmation);
            } else {
              reject(constants.USER_CANCELLED, payload);
            }
           }
        });
      }
    });
  }
};

var exported = {};
Object.assign(exported, constants, functions);

module.exports = exported;
