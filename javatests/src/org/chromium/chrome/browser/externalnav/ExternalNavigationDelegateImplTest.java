// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.externalnav;

import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Instrumentation tests for {@link ExternalNavigationHandler}.
 */
public class ExternalNavigationDelegateImplTest extends InstrumentationTestCase {

    private static List<ResolveInfo> makeResolveInfos(ResolveInfo... infos) {
        return Arrays.asList(infos);
    }

    @SmallTest
    public void testIsPackageSpecializedHandler_NoResolveInfo() {
        String packageName = "";
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        assertFalse(ExternalNavigationDelegateImpl
                .isPackageSpecializedHandler(resolveInfos, packageName));
    }

    @SmallTest
    public void testIsPackageSpecializedHandler_NoPathOrAuthority() {
        String packageName = "";
        ResolveInfo info = new ResolveInfo();
        info.filter = new IntentFilter();
        List<ResolveInfo> resolveInfos = makeResolveInfos(info);
        assertFalse(ExternalNavigationDelegateImpl
                .isPackageSpecializedHandler(resolveInfos, packageName));
    }

    @SmallTest
    public void testIsPackageSpecializedHandler_WithPath() {
        String packageName = "";
        ResolveInfo info = new ResolveInfo();
        info.filter = new IntentFilter();
        info.filter.addDataPath("somepath", 2);
        List<ResolveInfo> resolveInfos = makeResolveInfos(info);
        assertTrue(ExternalNavigationDelegateImpl
                .isPackageSpecializedHandler(resolveInfos, packageName));
    }

    @SmallTest
    public void testIsPackageSpecializedHandler_WithAuthority() {
        String packageName = "";
        ResolveInfo info = new ResolveInfo();
        info.filter = new IntentFilter();
        info.filter.addDataAuthority("http://www.google.com", "80");
        List<ResolveInfo> resolveInfos = makeResolveInfos(info);
        assertTrue(ExternalNavigationDelegateImpl
                .isPackageSpecializedHandler(resolveInfos, packageName));
    }

    @SmallTest
    public void testIsPackageSpecializedHandler_WithTargetPackage_Matching() {
        String packageName = "com.android.chrome";
        ResolveInfo info = new ResolveInfo();
        info.filter = new IntentFilter();
        info.filter.addDataAuthority("http://www.google.com", "80");
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = packageName;
        List<ResolveInfo> resolveInfos = makeResolveInfos(info);
        assertTrue(ExternalNavigationDelegateImpl
                .isPackageSpecializedHandler(resolveInfos, packageName));
    }

    @SmallTest
    public void testIsPackageSpecializedHandler_WithTargetPackage_NotMatching() {
        String packageName = "com.android.chrome";
        ResolveInfo info = new ResolveInfo();
        info.filter = new IntentFilter();
        info.filter.addDataAuthority("http://www.google.com", "80");
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = "com.foo.bar";
        List<ResolveInfo> resolveInfos = makeResolveInfos(info);
        assertFalse(ExternalNavigationDelegateImpl
                .isPackageSpecializedHandler(resolveInfos, packageName));
    }
}
