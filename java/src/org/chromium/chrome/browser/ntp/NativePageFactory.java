// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.ntp;

import android.app.Activity;
import android.net.Uri;

import org.chromium.base.VisibleForTesting;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.NativePage;
import org.chromium.chrome.browser.UrlConstants;
import org.chromium.chrome.browser.bookmarks.BookmarkPage;
import org.chromium.chrome.browser.physicalweb.PhysicalWebDiagnosticsPage;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.util.FeatureUtilities;

/**
 * Creates NativePage objects to show chrome-native:// URLs using the native Android view system.
 */
public class NativePageFactory {

    public static final String CHROME_NATIVE_SCHEME = "chrome-native";

    public static NativePageBuilder sNativePageBuilder = new NativePageBuilder();

    @VisibleForTesting
    static class NativePageBuilder {
        protected NativePage buildNewTabPage(Activity activity, Tab tab,
                TabModelSelector tabModelSelector) {
            if (tab.isIncognito()) {
                return new IncognitoNewTabPage(activity);
            } else {
                return new NewTabPage(activity, tab, tabModelSelector);
            }
        }

        protected NativePage buildNewTabPage(Activity activity, Tab tab,
                                             TabModelSelector tabModelSelector, String url) {
            if (tab != null && tab.isIncognito()) {
                return new IncognitoNewTabPage(activity);
            } else {
                return new BrowserNewTabPage(activity, tab, tabModelSelector, url);
            }
        }

        protected NativePage buildBookmarksPage(Activity activity, Tab tab) {
            return new BookmarkPage(activity, tab);
        }

        protected NativePage buildRecentTabsPage(Activity activity, Tab tab) {
            RecentTabsManager recentTabsManager = FeatureUtilities.isDocumentMode(activity)
                    ? new DocumentRecentTabsManager(tab, activity)
                    : new RecentTabsManager(tab, tab.getProfile(), activity);
            return new RecentTabsPage(activity, recentTabsManager);
        }

        protected NativePage buildPhysicalWebDiagnosticsPage(Activity activity) {
            return new PhysicalWebDiagnosticsPage(activity);
        }
    }

    enum NativePageType {
        NONE, CANDIDATE, NTP, BOOKMARKS, RECENT_TABS, PHYSICAL_WEB
    }

    public static NativePageType nativePageType(String url, NativePage candidatePage,
            boolean isIncognito) {
        if (url == null) return NativePageType.NONE;

        Uri uri = Uri.parse(url);
        if (!CHROME_NATIVE_SCHEME.equals(uri.getScheme())) {
            return NativePageType.NONE;
        }

        String host = uri.getHost();
        if (candidatePage != null && candidatePage.getHost().equals(host)) {
            return NativePageType.CANDIDATE;
        }

        if (UrlConstants.NTP_HOST.equals(host)) {
            return NativePageType.NTP;
        } else if (UrlConstants.BOOKMARKS_HOST.equals(host)) {
            return NativePageType.BOOKMARKS;
        } else if (UrlConstants.RECENT_TABS_HOST.equals(host) && !isIncognito) {
            return NativePageType.RECENT_TABS;
        } else if (UrlConstants.PHYSICAL_WEB_HOST.equals(host)) {
            if (ChromeFeatureList.isEnabled("PhysicalWeb")) {
                return NativePageType.PHYSICAL_WEB;
            } else {
                return NativePageType.NONE;
            }
        } else {
            return NativePageType.NONE;
        }
    }

    /**
     * Returns a NativePage for displaying the given URL if the URL is a valid chrome-native URL,
     * or null otherwise. If candidatePage is non-null and corresponds to the URL, it will be
     * returned. Otherwise, a new NativePage will be constructed.
     *
     * @param url The URL to be handled.
     * @param candidatePage A NativePage to be reused if it matches the url, or null.
     * @param tab The Tab that will show the page.
     * @param tabModelSelector The TabModelSelector containing the tab.
     * @param activity The activity used to create the views for the page.
     * @return A NativePage showing the specified url or null.
     */
    public static NativePage createNativePageForURL(String url, NativePage candidatePage,
            Tab tab, TabModelSelector tabModelSelector, Activity activity) {
        return createNativePageForURL(url, candidatePage, tab, tabModelSelector, activity,
                tab.isIncognito());
    }

    @VisibleForTesting
    static NativePage createNativePageForURL(String url, NativePage candidatePage,
            Tab tab, TabModelSelector tabModelSelector, Activity activity,
            boolean isIncognito) {
        NativePage page;

        switch (nativePageType(url, candidatePage, isIncognito)) {
            case NONE:
                return null;
            case CANDIDATE:
                page = candidatePage;
                break;
            case NTP:
                if (tab == null)
                    page = sNativePageBuilder.buildNewTabPage(activity, tab, tabModelSelector);
                else
                    page = sNativePageBuilder.buildNewTabPage(activity, tab, tabModelSelector, url);
                break;
            case BOOKMARKS:
                if (tab != null && !isIncognito && tab.isNativePage() &&
                        tab.getNativePage() instanceof BrowserNewTabPage) {
                    BrowserNewTabPage ntp = (BrowserNewTabPage) tab.getNativePage();
                    ntp.showBookmarksPage();
                    page = ntp;
                } else if (tab != null && !isIncognito) {
                    page = sNativePageBuilder.buildNewTabPage(activity, tab, tabModelSelector, url);
                } else
                    page = sNativePageBuilder.buildBookmarksPage(activity, tab);
                break;
            case RECENT_TABS:
                if (tab != null && !isIncognito && tab.isNativePage() &&
                        tab.getNativePage() instanceof BrowserNewTabPage) {
                    BrowserNewTabPage ntp = (BrowserNewTabPage) tab.getNativePage();
                    ntp.showRecentTabs();
                    page = ntp;
                } else if (tab != null && !isIncognito) {
                    page = sNativePageBuilder.buildNewTabPage(activity, tab, tabModelSelector, url);
                } else
                    page = sNativePageBuilder.buildRecentTabsPage(activity, tab);
                break;
            case PHYSICAL_WEB:
                page = sNativePageBuilder.buildPhysicalWebDiagnosticsPage(activity);
                break;
            default:
                assert false;
                return null;
        }
        page.updateForUrl(url);
        return page;
    }

    /**
     * Returns whether the URL would navigate to a native page.
     *
     * @param url The URL to be checked.
     * @param isIncognito Whether the page will be displayed in incognito mode.
     * @return Whether the host and the scheme of the passed in URL matches one of the supported
     *         native pages.
     */
    public static boolean isNativePageUrl(String url, boolean isIncognito) {
        return nativePageType(url, null, isIncognito) != NativePageType.NONE;
    }

    @VisibleForTesting
    static void setNativePageBuilderForTesting(NativePageBuilder builder) {
        sNativePageBuilder = builder;
    }
}
