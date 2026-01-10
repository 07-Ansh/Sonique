 
package com.liskovsoft.sharedutils.cronet;

import android.content.Context;
import androidx.annotation.IntDef;
import com.liskovsoft.sharedutils.mylogger.Log;
import org.chromium.net.CronetEngine;
import org.chromium.net.CronetProvider;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

 
public final class CronetEngineWrapper {

  private static final String TAG = "CronetEngineWrapper";

  private final CronetEngine cronetEngine;
  private final @CronetEngineSource int cronetEngineSource;

   
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({SOURCE_NATIVE, SOURCE_GMS, SOURCE_UNKNOWN, SOURCE_USER_PROVIDED, SOURCE_UNAVAILABLE})
  public @interface CronetEngineSource {}
   
  public static final int SOURCE_NATIVE = 0;
   
  public static final int SOURCE_GMS = 1;
   
  public static final int SOURCE_UNKNOWN = 2;
   
  public static final int SOURCE_USER_PROVIDED = 3;
   
  public static final int SOURCE_UNAVAILABLE = 4;

   
  public CronetEngineWrapper(Context context) {
    this(context, false);
  }

   
  public CronetEngineWrapper(Context context, boolean preferGMSCoreCronet) {
    CronetEngine cronetEngine = null;
    @CronetEngineSource int cronetEngineSource = SOURCE_UNAVAILABLE;
    List<CronetProvider> cronetProviders = new ArrayList<>(CronetProvider.getAllProviders(context));
     
    for (int i = cronetProviders.size() - 1; i >= 0; i--) {
      if (!cronetProviders.get(i).isEnabled()
          || CronetProvider.PROVIDER_NAME_FALLBACK.equals(cronetProviders.get(i).getName())) {
        cronetProviders.remove(i);
      }
    }
     
    CronetProviderComparator providerComparator = new CronetProviderComparator(preferGMSCoreCronet);
    Collections.sort(cronetProviders, providerComparator);
    for (int i = 0; i < cronetProviders.size() && cronetEngine == null; i++) {
      String providerName = cronetProviders.get(i).getName();
      try {
         
         
        cronetEngine = cronetProviders.get(i).createBuilder()
                .enableQuic(true)
                .enableHttp2(true)
                .enableBrotli(true)
                .build();
        if (providerComparator.isNativeProvider(providerName)) {
          cronetEngineSource = SOURCE_NATIVE;
        } else if (providerComparator.isGMSCoreProvider(providerName)) {
          cronetEngineSource = SOURCE_GMS;
        } else {
          cronetEngineSource = SOURCE_UNKNOWN;
        }
        Log.d(TAG, "CronetEngine built using " + providerName);
      } catch (SecurityException e) {
        Log.w(TAG, "Failed to build CronetEngine. Please check if current process has "
            + "android.permission.ACCESS_NETWORK_STATE.");
      } catch (UnsatisfiedLinkError e) {
        Log.w(TAG, "Failed to link Cronet binaries. Please check if native Cronet binaries are "
            + "bundled into your com.sonique.app.");
      }
    }
    if (cronetEngine == null) {
      Log.w(TAG, "Cronet not available. Using fallback provider.");
    }
    this.cronetEngine = cronetEngine;
    this.cronetEngineSource = cronetEngineSource;
  }

   
  public CronetEngineWrapper(CronetEngine cronetEngine) {
    this.cronetEngine = cronetEngine;
    this.cronetEngineSource = SOURCE_USER_PROVIDED;
  }

   
  public @CronetEngineSource int getCronetEngineSource() {
    return cronetEngineSource;
  }

   
  public CronetEngine getCronetEngine() {
    return cronetEngine;
  }

  private static class CronetProviderComparator implements Comparator<CronetProvider> {

    private final String gmsCoreCronetName;
    private final boolean preferGMSCoreCronet;

     
    @SuppressWarnings("UseMultiCatch")
    public CronetProviderComparator(boolean preferGMSCoreCronet) {
       
       
      String gmsCoreVersionString = null;
      try {
        Class<?> cronetProviderInstallerClass =
            Class.forName("com.google.android.gms.net.CronetProviderInstaller");
        Field providerNameField = cronetProviderInstallerClass.getDeclaredField("PROVIDER_NAME");
        gmsCoreVersionString = (String) providerNameField.get(null);
      } catch (ClassNotFoundException e) {
         
      } catch (NoSuchFieldException e) {
         
      } catch (IllegalAccessException e) {
         
      }
      gmsCoreCronetName = gmsCoreVersionString;
      this.preferGMSCoreCronet = preferGMSCoreCronet;
    }

    @Override
    public int compare(CronetProvider providerLeft, CronetProvider providerRight) {
      int typePreferenceLeft = evaluateCronetProviderType(providerLeft.getName());
      int typePreferenceRight = evaluateCronetProviderType(providerRight.getName());
      if (typePreferenceLeft != typePreferenceRight) {
        return typePreferenceLeft - typePreferenceRight;
      }
      return -compareVersionStrings(providerLeft.getVersion(), providerRight.getVersion());
    }

    public boolean isNativeProvider(String providerName) {
      return CronetProvider.PROVIDER_NAME_APP_PACKAGED.equals(providerName);
    }

    public boolean isGMSCoreProvider(String providerName) {
      return gmsCoreCronetName != null && gmsCoreCronetName.equals(providerName);
    }

     
    private int evaluateCronetProviderType(String providerName) {
      if (isNativeProvider(providerName)) {
        return 1;
      }
      if (isGMSCoreProvider(providerName)) {
        return preferGMSCoreCronet ? 0 : 2;
      }
       
      return -1;
    }

     
    private static int compareVersionStrings(String versionLeft, String versionRight) {
      if (versionLeft == null || versionRight == null) {
        return 0;
      }
      String[] versionStringsLeft = versionLeft.split("\\.", -1);
      String[] versionStringsRight = versionRight.split("\\.", -1);
      int minLength = Math.min(versionStringsLeft.length, versionStringsRight.length);
      for (int i = 0; i < minLength; i++) {
        if (!versionStringsLeft[i].equals(versionStringsRight[i])) {
          try {
            int versionIntLeft = Integer.parseInt(versionStringsLeft[i]);
            int versionIntRight = Integer.parseInt(versionStringsRight[i]);
            return versionIntLeft - versionIntRight;
          } catch (NumberFormatException e) {
            return 0;
          }
        }
      }
      return 0;
    }
  }

}
