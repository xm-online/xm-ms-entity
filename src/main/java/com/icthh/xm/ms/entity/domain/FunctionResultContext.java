package com.icthh.xm.ms.entity.domain;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.logging.util.MdcUtils;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.icthh.xm.commons.utils.ModelAndViewUtils.MVC_FUNC_RESULT;

public class FunctionResultContext extends FunctionContext implements FunctionResult {

    @Override
    public long getExecuteTime() {
        Instant startDate = getStartDate();
        Instant endDate = getEndDate();

        return startDate != null && endDate != null ? Duration.between(startDate, endDate).getSeconds() : 0;
    }

    @Override
    public ModelAndView getModelAndView() {
        return (ModelAndView) Optional.ofNullable(getData())
            .map(d -> d.get(MVC_FUNC_RESULT))
            .orElse(null);
    }
}
