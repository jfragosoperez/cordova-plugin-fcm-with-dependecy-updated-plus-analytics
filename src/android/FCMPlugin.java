package com.gae.scaffolder.plugin;

import android.content.Context;
import android.os.Bundle;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;
import java.util.Iterator;

public class FCMPlugin extends CordovaPlugin {

	private static final String TAG = "FCMPlugin";

	public static CordovaWebView gWebView;
	public static String notificationCallBack = "FCMPlugin.onNotificationReceived";
	public static String tokenRefreshCallBack = "FCMPlugin.onTokenRefreshReceived";
	public static Boolean notificationCallBackReady = false;
	public static Map<String, Object> lastPush = null;
	private FirebaseAnalytics firebaseAnalytics;

	public FCMPlugin() {
	}

	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		gWebView = webView;
		Log.d(TAG, "==> FCMPlugin initialize");
		// ANALYTICS
		Context context = cordova.getActivity().getApplicationContext();
		firebaseAnalytics = FirebaseAnalytics.getInstance(context);

		FirebaseMessaging.getInstance().subscribeToTopic("android");
		FirebaseMessaging.getInstance().subscribeToTopic("all");
	}

	public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext)
			throws JSONException {

		Log.d(TAG, "==> FCMPlugin execute: " + action);

		try {
			// READY //
			if (action.equals("ready")) {
				//
				callbackContext.success();
			}
			// GET INSTANCE ID //
			else if (action.equals("getId")) {
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						try {
							String id = FirebaseInstanceId.getInstance().getId();
							callbackContext.success(id);
							Log.d(TAG, "\tInstance id: " + id);
						} catch (Exception e) {
							Log.d(TAG, "\tError retrieving instance id");
						}
					}
				});
			}
			// DELETE INSTANCE ID //
			else if (action.equals("deleteInstanceId")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try {
							FirebaseInstanceId.getInstance().deleteInstanceId();
							callbackContext.success();
							Log.d(TAG, "\tInstance id deleted");
						} catch (Exception e) {
							Log.d(TAG, "\tError deleting instance id");
							callbackContext.error(e.getMessage());
						}
					}
				});
			}
			// GET TOKEN //
			else if (action.equals("getToken")) {
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						try {
							String token = FirebaseInstanceId.getInstance().getToken();
							callbackContext.success(FirebaseInstanceId.getInstance().getToken());
							Log.d(TAG, "\tToken: " + token);
						} catch (Exception e) {
							Log.d(TAG, "\tError retrieving token");
						}
					}
				});
			}
			// NOTIFICATION CALLBACK REGISTER //
			else if (action.equals("registerNotification")) {
				notificationCallBackReady = true;
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						if (lastPush != null)
							FCMPlugin.sendPushPayload(lastPush);
						lastPush = null;
						callbackContext.success();
					}
				});
			}
			// UN/SUBSCRIBE TOPICS //
			else if (action.equals("subscribeToTopic")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try {
							FirebaseMessaging.getInstance().subscribeToTopic(args.getString(0));
							callbackContext.success();
						} catch (Exception e) {
							callbackContext.error(e.getMessage());
						}
					}
				});
			} else if (action.equals("unsubscribeFromTopic")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try {
							FirebaseMessaging.getInstance().unsubscribeFromTopic(args.getString(0));
							callbackContext.success();
						} catch (Exception e) {
							callbackContext.error(e.getMessage());
						}
					}
				});
			} else if (action.equals("logEvent")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try {
							final String name = args.getString(0);
							final JSONObject params = args.getJSONObject(1);
							Bundle bundle = new Bundle();
							Iterator<String> it = params.keys();

							while (it.hasNext()) {
								String key = it.next();
								Object value = params.get(key);

								if (value instanceof String) {
									bundle.putString(key, (String) value);
								} else if (value instanceof Integer) {
									bundle.putInt(key, (Integer) value);
								} else if (value instanceof Double) {
									bundle.putDouble(key, (Double) value);
								} else if (value instanceof Long) {
									bundle.putLong(key, (Long) value);
								} else {
									Log.w(TAG, "Value for key " + key + " not one of (String, Integer, Double, Long)");
								}
							}

							firebaseAnalytics.logEvent(name, bundle);

							callbackContext.success();
						} catch (Exception e) {
							callbackContext.error(e.getMessage());
						}
					}
				});
			} else if (action.equals("setUserId")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try {
							firebaseAnalytics.setUserId(args.getString(0));
							callbackContext.success();
						} catch (Exception e) {
							callbackContext.error(e.getMessage());
						}
					}
				});
			} else if (action.equals("setUserProperty")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try {
							firebaseAnalytics.setUserProperty(args.getString(0), args.getString(1));
							callbackContext.success();
						} catch (Exception e) {
							callbackContext.error(e.getMessage());
						}
					}
				});
			} else if (action.equals("setCurrentScreen")) {
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						try {
							firebaseAnalytics.setCurrentScreen(cordova.getActivity(), args.getString(0), null);
							callbackContext.success();
						} catch (Exception e) {
							callbackContext.error(e.getMessage());
						}
					}
				});
			} else {
				callbackContext.error("Method not found");
				return false;
			}
		} catch (Exception e) {
			Log.d(TAG, "ERROR: onPluginAction: " + e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		}

		// cordova.getThreadPool().execute(new Runnable() {
		// public void run() {
		// //
		// }
		// });

		// cordova.getActivity().runOnUiThread(new Runnable() {
		// public void run() {
		// //
		// }
		// });
		return true;
	}

	public static void sendPushPayload(Map<String, Object> payload) {
		Log.d(TAG, "==> FCMPlugin sendPushPayload");
		Log.d(TAG, "\tnotificationCallBackReady: " + notificationCallBackReady);
		Log.d(TAG, "\tgWebView: " + gWebView);
		try {
			JSONObject jo = new JSONObject();
			for (String key : payload.keySet()) {
				jo.put(key, payload.get(key));
				Log.d(TAG, "\tpayload: " + key + " => " + payload.get(key));
			}
			String callBack = "javascript:" + notificationCallBack + "(" + jo.toString() + ")";
			if (notificationCallBackReady && gWebView != null) {
				Log.d(TAG, "\tSent PUSH to view: " + callBack);
				gWebView.sendJavascript(callBack);
			} else {
				Log.d(TAG, "\tView not ready. SAVED NOTIFICATION: " + callBack);
				lastPush = payload;
			}
		} catch (Exception e) {
			Log.d(TAG, "\tERROR sendPushToView. SAVED NOTIFICATION: " + e.getMessage());
			lastPush = payload;
		}
	}

	public static void sendTokenRefresh(String token) {
		Log.d(TAG, "==> FCMPlugin sendRefreshToken");
		try {
			String callBack = "javascript:" + tokenRefreshCallBack + "('" + token + "')";
			gWebView.sendJavascript(callBack);
		} catch (Exception e) {
			Log.d(TAG, "\tERROR sendRefreshToken: " + e.getMessage());
		}
	}

	@Override
	public void onDestroy() {
		gWebView = null;
		notificationCallBackReady = false;
	}
}
