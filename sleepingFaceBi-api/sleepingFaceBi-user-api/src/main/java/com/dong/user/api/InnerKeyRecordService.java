package com.dong.user.api;

import com.dong.user.api.model.entity.KeyRecord;
import com.dong.user.api.model.vo.KeyRecordVO;



public interface InnerKeyRecordService {

    KeyRecordVO getKeyRecordVO(KeyRecord keyRecord);

    KeyRecord getKeyRecord(String ak);
}
