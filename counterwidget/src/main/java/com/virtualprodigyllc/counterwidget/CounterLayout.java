package com.virtualprodigyllc.counterwidget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by virtualprodigyllc on 8/8/15.
 */
public class CounterLayout extends RelativeLayout {

    private EditText counterText;
    private ImageButton countUpButton;
    private ImageButton countDownButton;
    private CounterCallback callbackListener;

    private Timer timerTask;
    private int upperLimit = 99;
    private int lowerLimit = 0;

    private final int halfSecond = 500;
    /**
     * Also long as the user is pushing the button, this flag is set allow long press functionality to begin
     */
    private boolean isButtonPressed = false;
    /***
     * timerCount is used to keep track of the value being updated during a long count.
     * it should be set to zero when the timer stops and assigned a value before starting the timer
     */
    private int timerCount;
    /**
     * timerFactor is the value to be added or subtracted from the current value during a time based count
     */
    private int timerFactor;
    private final String TAG = this.getClass().getSimpleName();

    public interface CounterCallback {
        /**
         * This method is called when the user interacts with the counter
         *
         * @param wasUpdated
         */
        public void counterUpdated(boolean wasUpdated);
    }


    public CounterLayout(Context context) {
        super(context);
        inflateCustomView();
    }

    public CounterLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateCustomView();
    }

    public CounterLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateCustomView();
    }

    private void inflateCustomView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.counter_layout, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        counterText = (EditText) findViewById(R.id.counterEditText);
        countUpButton = (ImageButton) findViewById(R.id.positiveButton);
        countUpButton.setOnTouchListener(onTouchListener);
        countDownButton = (ImageButton) findViewById(R.id.negativeButton);
        countDownButton.setOnTouchListener(onTouchListener);

    }

    /**
     * Sets a listener to be called with callbacks fromt he counter
     */
    public void setCallbackListener(CounterCallback listener) {
        this.callbackListener = listener;
    }

    View.OnTouchListener onTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isButtonPressed = true;

                    if (callbackListener != null) {
                        callbackListener.counterUpdated(true);
                    }

                    try {
                        timerCount = Integer.parseInt(counterText.getText().toString());
                    } catch (Exception e) {
                        // logging error && exiting
                        Log.e(TAG, "Failed parse value", e);
                        return false;
                    }

                    if (view == countUpButton) {
                        timerFactor = 1;

                        if (timerCount < upperLimit) {
                            counterText.setText("" + (timerCount + 1));
                             /* the user continues to hold the button it is a long pressed
                            and the counter should increment it's value ever 300ms by 1*/
                            timeBaseCounting();
                        }


                    } else if (view == countDownButton) {
                        timerFactor = -1;
                        if (timerCount > 0) {
                            counterText.setText("" + (timerCount - 1));
                            /* the user continues to hold the button it is a long pressed
                            and the counter should increment it's value ever 300ms by 1*/
                            timeBaseCounting();
                        }
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    isButtonPressed = false;
                    stopTimBasedCounting();
                    return true;
            }
            return false;

        }
    };

    /**
     * This method updates the count on a fixed interval
     */
    private void timeBaseCounting() {
        if(timerTask != null){
            //clear any possible conflicting counters
            stopTimBasedCounting();
        }

        timerTask = new Timer();
        timerTask.schedule(new TimerTask() {
            @Override
            public void run() {

                if (isButtonPressed) {//the user continued to press the button, start auto in/decrementing count
                    //in/decrement the value once before the auto in/decrementing starts.
                    // This provides the user will a visual hint of what;s going to happen
                    timerCount = timerCount + timerFactor;
                    if (timerCount <= upperLimit && timerCount >= lowerLimit) {
                        updateCountOnUIThread(timerCount);
                    }

                    final long repeatInterval = 200;
                    final long startDelay = 250;

                    timerTask.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            timerCount = timerCount + timerFactor;
                            if (timerCount <= upperLimit && timerCount >= lowerLimit) {
                                updateCountOnUIThread(timerCount);
                            }
                        }
                    }, startDelay, repeatInterval);
                }
            }
        }, halfSecond);
    }

    /**
     * This method updates the counter on the ui thread when called from a worker thread
     * @param value - the value to be displayed
     */
    private synchronized void updateCountOnUIThread(final int value){
        counterText.post(new Runnable() {
            @Override
            public void run() {
                counterText.setText("" + value);

            }
        });

    }
    /**
     * This method stops the timer that's updating the count on a fixed interval
     */
    private void stopTimBasedCounting() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask.purge();
            timerTask = null;
            timerCount = 0;
        }

    }

    View.OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == countUpButton) {
                int value;
                callbackListener.counterUpdated(true);
                try {
                    value = Integer.parseInt(counterText.getText().toString());
                    if (value < 99) {
                        counterText.setText("" + (value + 1));
                    }
                } catch (Exception e) {
                    value = 1;
                }
            } else if (v == countDownButton) {
                callbackListener.counterUpdated(true);
                int value;
                try {
                    value = Integer.parseInt(counterText.getText().toString());
                    if (value > 0) {
                        counterText.setText("" + (value - 1));
                    }
                } catch (Exception e) {
                    value = 1;
                }
            }
        }
    };

    /**
     * This method hides or displays the counter buttons
     * Use the standard View.VISIBLE, View.INVISIBLE, View.GONE
     *
     * @param visibility
     */
    public void setCounterButtonVisibility(int visibility) {
        countUpButton.setVisibility(visibility);
        countDownButton.setVisibility(visibility);
    }

    /**
     * Sets the maximum and minimum value the counter will be allowed to reach
     *
     * @param upperLimit - highest possible value, by default it's 99
     * @param lowerLimit - lowest possible value, by default it's 0
     */
    public void setCounterLimits(int upperLimit, int lowerLimit) {
        this.upperLimit = upperLimit;
        this.lowerLimit = lowerLimit;
    }

    /**
     * This method returns the value displayed in the counter
     *
     * @return If the value can not be parsed or no value is enter, a zero is returned
     */
    public int getCount() {
        return -1;
    }
}
