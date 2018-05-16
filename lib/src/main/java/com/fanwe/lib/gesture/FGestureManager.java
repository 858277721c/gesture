/*
 * Copyright (C) 2017 zhengjun, fanwe (http://www.fanwe.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fanwe.lib.gesture;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class FGestureManager
{
    private VelocityTracker mVelocityTracker;
    private boolean mHasConsumeEvent;
    private final FTagTouchHelper mTouchHelper = new FTagTouchHelper()
    {
        @Override
        protected void onTagInterceptChanged(boolean tagIntercept)
        {
            super.onTagInterceptChanged(tagIntercept);
            mCallback.onTagInterceptChanged(tagIntercept);
        }

        @Override
        protected void onTagConsumeChanged(boolean tagConsume)
        {
            super.onTagConsumeChanged(tagConsume);
            mCallback.onTagConsumeChanged(tagConsume);
        }
    };

    private final Callback mCallback;

    public FGestureManager(Callback callback)
    {
        if (callback == null) throw new NullPointerException("callback is null");
        mCallback = callback;
    }

    private VelocityTracker getVelocityTracker()
    {
        if (mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        return mVelocityTracker;
    }

    private void releaseVelocityTracker()
    {
        if (mVelocityTracker != null)
        {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 返回触摸帮助类
     *
     * @return
     */
    public FTouchHelper getTouchHelper()
    {
        return mTouchHelper;
    }

    /**
     * 一次完整的按下到离开的触摸过程中，是否有消费过事件
     *
     * @return
     */
    public boolean hasConsumeEvent()
    {
        return mHasConsumeEvent;
    }

    /**
     * 当前是否处于消费中
     *
     * @return
     */
    public boolean isTagConsume()
    {
        return mTouchHelper.isTagConsume();
    }

    /**
     * 是否是点击事件
     *
     * @param event
     * @param context
     * @return
     */
    public boolean isClick(MotionEvent event, Context context)
    {
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            final long clickTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout();
            final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

            final long duration = event.getEventTime() - event.getDownTime();
            final int dx = (int) getTouchHelper().getDeltaXFrom(FTagTouchHelper.EVENT_DOWN);
            final int dy = (int) getTouchHelper().getDeltaYFrom(FTagTouchHelper.EVENT_DOWN);

            if (duration < clickTimeout && dx < touchSlop && dy < touchSlop)
            {
                return true;
            }
        }

        return false;
    }

    private void reset()
    {
        releaseVelocityTracker();
        mHasConsumeEvent = false;
    }

    /**
     * 外部调用
     *
     * @param event
     * @return
     */
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        mTouchHelper.processTouchEvent(event);
        getVelocityTracker().addMovement(event);

        switch (event.getAction())
        {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                reset();
                break;
            default:
                if (mCallback.shouldInterceptTouchEvent(event))
                {
                    mTouchHelper.setTagIntercept(true);
                }
                break;
        }

        return mTouchHelper.isTagIntercept();
    }

    /**
     * 外部调用
     *
     * @param event
     * @return
     */
    public boolean onTouchEvent(MotionEvent event)
    {
        mTouchHelper.processTouchEvent(event);
        getVelocityTracker().addMovement(event);

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                return mCallback.consumeDownEvent(event);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mCallback.onConsumeEventFinish(event, getVelocityTracker());
                reset();
                break;
            default:
                if (mTouchHelper.isTagConsume())
                {
                    final boolean consume = mCallback.onConsumeEvent(event);
                    mTouchHelper.setTagConsume(consume);

                    if (consume)
                    {
                        mHasConsumeEvent = true;
                    }
                } else
                {
                    final boolean shouldConsume = mCallback.shouldConsumeTouchEvent(event);
                    mTouchHelper.setTagConsume(shouldConsume);
                }
                break;
        }

        return mTouchHelper.isTagConsume();
    }

    public abstract static class Callback
    {
        /**
         * 是否开始拦截事件({@link #onInterceptTouchEvent(MotionEvent)}方法触发)
         *
         * @param event
         * @return
         */
        public boolean shouldInterceptTouchEvent(MotionEvent event)
        {
            return false;
        }

        /**
         * 是否需要拦截发生变化
         *
         * @param tagIntercept
         */
        public void onTagInterceptChanged(boolean tagIntercept)
        {
        }

        /**
         * 是否需要消费按下事件，只有此方法返回true，才有后续的移动事件，默认返回true
         *
         * @param event
         * @return
         */
        public boolean consumeDownEvent(MotionEvent event)
        {
            return true;
        }

        /**
         * 是否开始消费事件
         *
         * @param event
         * @return
         */
        public abstract boolean shouldConsumeTouchEvent(MotionEvent event);

        /**
         * 是否需要消费发生变化
         *
         * @param tagConsume
         */
        public void onTagConsumeChanged(boolean tagConsume)
        {
        }

        /**
         * 事件回调
         *
         * @param event
         * @return
         */
        public abstract boolean onConsumeEvent(MotionEvent event);

        /**
         * 事件结束回调，收到{@link MotionEvent#ACTION_UP}或者{@link MotionEvent#ACTION_CANCEL}
         *
         * @param event
         * @param velocityTracker
         */
        public abstract void onConsumeEventFinish(MotionEvent event, VelocityTracker velocityTracker);
    }
}
