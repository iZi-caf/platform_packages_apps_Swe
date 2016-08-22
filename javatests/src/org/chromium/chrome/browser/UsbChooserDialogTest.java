// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser;

import android.app.Dialog;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import org.chromium.base.ThreadUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.test.ChromeActivityTestCaseBase;
import org.chromium.components.security_state.ConnectionSecurityLevel;
import org.chromium.content.browser.test.util.Criteria;
import org.chromium.content.browser.test.util.CriteriaHelper;
import org.chromium.content.browser.test.util.TouchCommon;
import org.chromium.ui.widget.TextViewWithClickableSpans;

import java.util.concurrent.Callable;

/**
 * Tests for the UsbChooserDialog class.
 */
public class UsbChooserDialogTest extends ChromeActivityTestCaseBase<ChromeActivity> {
    /**
     * Works like the UsbChooserDialog class, but records calls to native methods instead of
     * calling back to C++.
     */
    static class UsbChooserDialogWithFakeNatives extends UsbChooserDialog {
        String mSelectedDeviceId = "";

        UsbChooserDialogWithFakeNatives() {
            super(42 /* nativeUsbChooserDialogPtr */);
        }

        @Override
        void nativeOnItemSelected(long nativeUsbChooserDialogAndroid, String deviceId) {
            mSelectedDeviceId = deviceId;
        }

        @Override
        void nativeOnDialogCancelled(long nativeUsbChooserDialogAndroid) {}

        @Override
        void nativeLoadUsbHelpPage(long nativeUsbChooserDialogAndroid) {}
    }

    private UsbChooserDialogWithFakeNatives mChooserDialog;

    public UsbChooserDialogTest() {
        super(ChromeActivity.class);
    }

    // ChromeActivityTestCaseBase:

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mChooserDialog = createDialog();
    }

    @Override
    public void startMainActivity() throws InterruptedException {
        startMainActivityOnBlankPage();
    }

    private UsbChooserDialogWithFakeNatives createDialog() {
        return ThreadUtils.runOnUiThreadBlockingNoException(
                new Callable<UsbChooserDialogWithFakeNatives>() {
                    @Override
                    public UsbChooserDialogWithFakeNatives call() {
                        UsbChooserDialogWithFakeNatives dialog =
                                new UsbChooserDialogWithFakeNatives();
                        dialog.show(getActivity(), "https://origin.example.com/",
                                ConnectionSecurityLevel.SECURE);
                        return dialog;
                    }
                });
    }

    private static void selectItem(final UsbChooserDialogWithFakeNatives chooserDialog,
            int position) throws InterruptedException {
        final Dialog dialog = chooserDialog.mItemChooserDialog.getDialogForTesting();
        final ListView items = (ListView) dialog.findViewById(R.id.items);
        final Button button = (Button) dialog.findViewById(R.id.positive);

        CriteriaHelper.pollUiThread(new Criteria() {
            @Override
            public boolean isSatisfied() {
                return items.getChildAt(0) != null;
            }
        });

        // The actual index for the first item displayed on screen.
        int firstVisiblePosition = items.getFirstVisiblePosition();
        TouchCommon.singleClickView(items.getChildAt(position - firstVisiblePosition));

        CriteriaHelper.pollUiThread(new Criteria() {
            @Override
            public boolean isSatisfied() {
                return button.isEnabled();
            }
        });

        TouchCommon.singleClickView(button);

        CriteriaHelper.pollUiThread(new Criteria() {
            @Override
            public boolean isSatisfied() {
                return !chooserDialog.mSelectedDeviceId.equals("");
            }
        });
    }

    /**
     * The messages include <link> ... </link> or <link1> ... </link1>, <link2> ... </link2>
     * sections that are used to create clickable spans. For testing the messages, this function
     * returns the raw string without the tags.
     */
    private static String removeLinkTags(String message) {
        return message.replaceAll("</?link1>", "").replaceAll(
                "</?link2>", "").replaceAll("</?link>", "");
    }

    @SmallTest
    public void testCancel() throws InterruptedException {
        Dialog dialog = mChooserDialog.mItemChooserDialog.getDialogForTesting();
        assertTrue(dialog.isShowing());

        final ListView items = (ListView) dialog.findViewById(R.id.items);
        final Button button = (Button) dialog.findViewById(R.id.positive);

        // The 'Connect' button should be disabled and the list view should be hidden.
        assertFalse(button.isEnabled());
        assertEquals(View.GONE, items.getVisibility());

        dialog.dismiss();

        CriteriaHelper.pollUiThread(new Criteria() {
            @Override
            public boolean isSatisfied() {
                return mChooserDialog.mSelectedDeviceId.equals("");
            }
        });
    }

    @SmallTest
    public void testSelectItem() throws InterruptedException {
        Dialog dialog = mChooserDialog.mItemChooserDialog.getDialogForTesting();

        TextViewWithClickableSpans statusView =
                (TextViewWithClickableSpans) dialog.findViewById(R.id.status);
        final ListView items = (ListView) dialog.findViewById(R.id.items);
        final Button button = (Button) dialog.findViewById(R.id.positive);
        final int position = 1;

        ThreadUtils.runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                mChooserDialog.addDevice("device_id_0", "device_name_0");
                mChooserDialog.addDevice("device_id_1", "device_name_1");
                mChooserDialog.addDevice("device_id_2", "device_name_2");
                // Show the desired position at the top of the ListView (in case
                // not all the items can fit on small devices' screens).
                items.setSelection(position);
            }
        });

        // After adding items to the dialog, the help message should be showing,
        // the 'Connect' button should still be disabled (since nothing's selected),
        // and the list view should show.
        assertEquals(removeLinkTags(getActivity().getString(
                R.string.usb_chooser_dialog_footnote_text)),
                statusView.getText().toString());
        assertFalse(button.isEnabled());
        assertEquals(View.VISIBLE, items.getVisibility());

        selectItem(mChooserDialog, position);

        assertEquals("device_id_1", mChooserDialog.mSelectedDeviceId);
    }
}
