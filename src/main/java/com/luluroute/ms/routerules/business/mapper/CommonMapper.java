package com.luluroute.ms.routerules.business.mapper;


import com.luluroute.ms.routerules.business.exceptions.MappingException;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.util.Constants;
import com.luluroute.ms.routerules.business.util.DateUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
//@Mapper
public class CommonMapper {
    public static String map(CharSequence value) {
        if (!ObjectUtils.isEmpty(value))
            return value.toString();
        return null;
    }

    public static int toInt() {
        return -1;
    }


    /**
     * Custom mapping to transform the date
     *
     * @param strDate
     * @return
     * @throws MappingException
     */
    public static Date convertToDate(CharSequence strDate, String field) throws MappingFormatException {
        if (!ObjectUtils.isEmpty(strDate))
            return DateUtil.convertToDate(strDate.toString(), field);
        return Constants.NULL;
    }

}
