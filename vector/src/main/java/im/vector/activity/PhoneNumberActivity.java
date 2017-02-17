/*
 * Copyright 2017 Vector Creations Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import im.vector.R;
import im.vector.util.PhoneNumberUtils;

public class PhoneNumberActivity extends AppCompatActivity implements TextView.OnEditorActionListener, TextWatcher, View.OnClickListener {

    private static final String LOG_TAG = PhoneNumberActivity.class.getSimpleName();
    private static final int REQUEST_COUNTRY = 1245;

    private TextInputEditText mCountry;
    private TextInputLayout mCountryLayout;

    private TextInputEditText mPhoneNumber;
    private TextInputLayout mPhoneNumberLayout;

    // Ex "FR"
    private String mCurrentRegionCode;
    // Ex "+33"
    private String mCurrentPhonePrefix;
    private Phonenumber.PhoneNumber mCurrentPhoneNumber;

     /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static void start(final Context context) {
        final Intent intent = new Intent(context, PhoneNumberActivity.class);
        context.startActivity(intent);
    }

    /*
    * *********************************************************************************************
    * Activity lifecycle
    * *********************************************************************************************
    */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_phone_number);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mCountry = (TextInputEditText) findViewById(R.id.phone_number_country_value);
        mCountryLayout = (TextInputLayout) findViewById(R.id.phone_number_country);
        mPhoneNumber = (TextInputEditText) findViewById(R.id.phone_number_value);
        mPhoneNumberLayout = (TextInputLayout) findViewById(R.id.phone_number);

        initViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_phone_number, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_add_phone_number:
                submitPhoneNumber();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_COUNTRY && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra(CountryPickerActivity.SELECTED_COUNTRY_NAME)
                    && data.hasExtra(CountryPickerActivity.SELECTED_COUNTRY_INDICATOR)) {
                mCountryLayout.setError(null);
                mCountryLayout.setErrorEnabled(false);

                if (TextUtils.isEmpty(mPhoneNumber.getText())) {
                    setCountryCode(data.getStringExtra(CountryPickerActivity.SELECTED_COUNTRY_CODE));
                    initPhoneWithPrefix();
                } else {
                    // Clear old prefix from phone before assigning new one
                    String updatedPhone = mPhoneNumber.getText().toString();
                    if (mCurrentPhonePrefix != null && updatedPhone.startsWith(mCurrentPhonePrefix)) {
                        updatedPhone = updatedPhone.substring(mCurrentPhonePrefix.length());
                    }
                    setCountryCode(data.getStringExtra(CountryPickerActivity.SELECTED_COUNTRY_CODE));

                    if (TextUtils.isEmpty(updatedPhone)) {
                        initPhoneWithPrefix();
                    } else {
                        formatPhoneNumber(updatedPhone);
                    }
                }
            }
        }
    }

    /*
    * *********************************************************************************************
    * Utils
    * *********************************************************************************************
    */

    private void initViews() {
        setCountryCode(null);

        initPhoneWithPrefix();

        mCountry.setOnClickListener(this);
        mPhoneNumber.setOnEditorActionListener(this);
        mPhoneNumber.addTextChangedListener(this);
    }

    private void setCountryCode(final String newCountryCode) {
        if (!TextUtils.isEmpty(newCountryCode) && !newCountryCode.equals(mCurrentRegionCode)) {
            mCurrentRegionCode = newCountryCode;
            mCountry.setText(PhoneNumberUtils.getHumanCountryCode(mCurrentRegionCode));
            // Update the prefix
            final int prefix = PhoneNumberUtil.getInstance().getCountryCodeForRegion(mCurrentRegionCode);
            if (prefix > 0) {
                mCurrentPhonePrefix = "+" + prefix;
            }
        }
    }

    private void initPhoneWithPrefix() {
        if (!TextUtils.isEmpty(mCurrentPhonePrefix)) {
            mPhoneNumber.setText(mCurrentPhonePrefix);
            mPhoneNumber.setSelection(mPhoneNumber.getText().length());
        }
    }

    private void formatPhoneNumber(final String rawPhoneNumber) {
        if (!TextUtils.isEmpty(mCurrentRegionCode)) {
            try {
                mCurrentPhoneNumber = PhoneNumberUtil.getInstance().parse(rawPhoneNumber.trim(), mCurrentRegionCode);
                final String formattedNumber = PhoneNumberUtil.getInstance().format(mCurrentPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                if (!TextUtils.equals(formattedNumber, mPhoneNumber.getText())) {
                    // Update field with the formatted number
                    mPhoneNumber.setText(formattedNumber);
                    mPhoneNumber.setSelection(mPhoneNumber.getText().length());
                }
            } catch (NumberParseException e) {
                mCurrentPhoneNumber = null;
            }
        }
    }

    private void submitPhoneNumber() {
        if (mCurrentRegionCode == null) {
            mCountryLayout.setErrorEnabled(true);
            mCountryLayout.setError(getString(R.string.settings_phone_number_country_error));
        } else {
            if (mCurrentPhoneNumber == null || !PhoneNumberUtil.getInstance().isValidNumberForRegion(mCurrentPhoneNumber, mCurrentRegionCode)) {
                mPhoneNumberLayout.setErrorEnabled(true);
                mPhoneNumberLayout.setError(getString(R.string.settings_phone_number_error));
            } else {
                //TODO phone is valid
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    /*
    * *********************************************************************************************
    * Listeners
    * *********************************************************************************************
    */

    @Override
    public void onClick(View v) {
        Intent i = new Intent(PhoneNumberActivity.this, CountryPickerActivity.class);
        startActivityForResult(i, REQUEST_COUNTRY);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE && !isFinishing()) {
            submitPhoneNumber();
            return true;
        }
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        formatPhoneNumber(s.toString());
        if (mPhoneNumberLayout.getError() != null) {
            mPhoneNumberLayout.setError(null);
            mPhoneNumberLayout.setErrorEnabled(false);
        }
    }
}