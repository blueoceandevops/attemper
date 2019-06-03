package com.github.attemper.common.param.dispatch.trigger.sub;

import lombok.Data;

@Data
public class CalendarIntervalTriggerParam extends CommonTriggerParam{

    protected int interval = 1;

    protected String timeUnit;

    protected int repeatCount = -1;

    protected boolean preserveDayLight;

    protected boolean skipDayIfNoHour;

    protected String timeZoneId;

}
