/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.message.bus.filter;

import io.nuls.protocol.message.base.BaseMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Charlie
 */
public class NulsMessageFilterChain {
    private List<NulsMessageFilter> list = new ArrayList<>();
    private ThreadLocal<Integer> index = new ThreadLocal<>();

    public boolean startDoFilter(BaseMessage message) {
        index.set(-1);
        doFilter(message);
        boolean result = index.get() == list.size();
        index.remove();
        return result;

    }

    public void doFilter(BaseMessage message) {
        index.set(1 + index.get());
        if (index.get() == list.size()) {
            return;
        }
        NulsMessageFilter filter = list.get(index.get());
        filter.doFilter(message, this);
    }

    public void addFilter(NulsMessageFilter<? extends BaseMessage> filter) {
        list.add(0, filter);
    }
}
