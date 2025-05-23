package com.newcalllib.expandingCapacity;

import com.newcalllib.expandingCapacity.IExpandingCapacityCallback;

interface IExpandingCapacity {
    void request(String content);
    void setCallback(in IExpandingCapacityCallback l);
}