package com.newcalllib.datachannel.V1_0;

import com.newcalllib.datachannel.V1_0.IImsDataChannelCallback;

interface IImsDataChannelServiceController {

    void createImsDataChannel(in String[] dcLabels,
            String appInfoXml, int slotId, String telecommCallId, String remotePhoneNumber);

    void setImsDataChannelCallback(in IImsDataChannelCallback l, int slotId, String telecommCallId);

    void setModemCallId(int slotId, int modemCallId, String telecomCallId);
}
