/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.backend.android;

/**
 * A class to hold hard coded constants. Developers may modify the values based
 * on their usage.
 *
 */
public interface Consts {

  /**
   * Set Project ID of your Google APIs Console Project.
   */
  public static final String PROJECT_ID = "x7-keen-enigma-y";

  /**
   * Set Project Number of your Google APIs Console Project.
   */
  public static final String PROJECT_NUMBER = "265493219963";

  /**
   * Set your Web Client ID for authentication at backend.
   */
  public static final String WEB_CLIENT_ID = "265493219963-8559tbm9mlrr8m5k5mil4t58b9ucclar.apps.googleusercontent.com";

  /**
   * Set default user authentication enabled or disabled.
   */
  public static final boolean IS_AUTH_ENABLED = true;

  /**
   * Auth audience for authentication on backend.
   */
  public static final String AUTH_AUDIENCE = "server:client_id:" + WEB_CLIENT_ID;

  /**
   * Endpoint root URL
   */
  public static final String ENDPOINT_ROOT_URL = "https://" + PROJECT_ID + ".appspot.com/_ah/api/";

  /**
   * A flag to switch if the app should be run with local dev server or
   * production (cloud).
   */
  public static final boolean LOCAL_ANDROID_RUN = false;

  /**
   * SharedPreference key for CloudBackend.
   */
  public static final String PREF_KEY_CLOUD_BACKEND = "PREF_KEY_CLOUD_BACKEND";

  /**
   * Tag name for logging.
   */
  public static final String TAG = "CloudBackend";
}