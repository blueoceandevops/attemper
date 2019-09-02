package com.github.attemper.core.service.dispatch;

import com.github.attemper.common.enums.ArgType;
import com.github.attemper.common.param.dispatch.arg.ext.SqlArgParam;
import com.github.attemper.common.param.dispatch.arg.ext.TradeDateArgParam;
import com.github.attemper.common.param.dispatch.job.JobArgListParam;
import com.github.attemper.common.param.dispatch.job.JobNameParam;
import com.github.attemper.common.param.dispatch.job.JobProjectSaveParam;
import com.github.attemper.common.result.app.project.Project;
import com.github.attemper.common.result.dispatch.arg.Arg;
import com.github.attemper.common.result.dispatch.job.ArgAllocatedResult;
import com.github.attemper.common.result.dispatch.job.Job;
import com.github.attemper.config.base.util.BeanUtil;
import com.github.attemper.core.dao.dispatch.JobMapper;
import com.github.attemper.sys.service.BaseServiceAdapter;
import com.github.attemper.sys.util.PageUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class JobService extends BaseServiceAdapter {

    @Autowired
    private JobMapper mapper;

    /**
     * get job by jobName
     * @param param
     * @return
     */
    public Job get(JobNameParam param) {
        Map<String, Object> paramMap = injectTenantIdToMap(param);
        return mapper.get(paramMap);
    }

    public Project getProject(JobNameParam param) {
        Map<String, Object> paramMap = injectTenantIdToMap(param);
        return mapper.getProject(paramMap);
    }

    public Void saveProject(JobProjectSaveParam param) {
        Map<String, Object> paramMap = injectTenantIdToMap(param);
        mapper.saveProject(paramMap);
        return null;
    }

    public Map<String, Object> listArg(JobArgListParam param) {
        Map<String, Object> paramMap = injectTenantIdToMap(param);
        PageHelper.startPage(param.getCurrentPage(), param.getPageSize());
        Page<ArgAllocatedResult> list = (Page<ArgAllocatedResult>) mapper.listArg(paramMap);
        return PageUtil.toResultMap(list);
    }

    public Job get(String jobName, String tenantId) {
        return mapper.get(injectTenantIdToMap(new JobNameParam().setJobName(jobName), tenantId));
    }

    public List<Arg> getAllArg(String jobName, String tenantId) {
        return mapper.getAllArg(injectTenantIdToMap(new JobNameParam().setJobName(jobName), tenantId));
    }

    /**
     * get project by jobName and tenantId
     *
     * @param jobName
     * @param tenantId
     * @return
     */
    public Project getProject(String jobName, String tenantId) {
        return mapper.getProject(injectTenantIdToMap(new JobNameParam().setJobName(jobName), tenantId));
    }

    public String getJsonArg(JobNameParam jobNameParam) {
        Map<String, Object> map = transArgToMap(jobNameParam.getJobName(), injectTenantId());
        return map.isEmpty() ? null : BeanUtil.bean2JsonStr(map);
    }

    @Autowired
    private ArgService argService;

    public Map<String, Object> transArgToMap(String jobName, String tenantId) {
        Map<String, Object> varMap = new HashMap<>();
        List<Arg> args = getAllArg(jobName, tenantId);
        for (Arg arg : args) {
            if (arg.getArgValue() != null) {
                if (ArgType.get(arg.getArgType()) == null) {
                    arg.setArgType(ArgType.STRING.getValue());
                }
                Object realArgValue;
                switch (ArgType.get(arg.getArgType())) {
                    case TRADE_DATE:
                        realArgValue = argService.getTradeDate(
                                new TradeDateArgParam()
                                        .setCalendarName(arg.getAttribute())
                                        .setExpression(arg.getArgValue()));
                        break;
                    case GIST:
                        realArgValue = argService.getGistCode(arg.getArgValue(), tenantId);
                        break;
                    case SQL:
                        List<Map<String, Object>> mapList = argService.getSqlResult(
                                new SqlArgParam()
                                        .setDbName(arg.getAttribute())
                                        .setSql(arg.getArgValue()));
                        if (mapList.size() == 1 && mapList.get(0).values() != null) {
                            realArgValue = mapList.get(0).values().toArray()[0];
                        } else {
                            realArgValue = mapList;
                        }
                        break;
                    case LIST:
                        realArgValue = toListValue(arg);
                        break;
                    case MAP:
                        realArgValue = toMapValue(arg);
                        break;
                    default: //string date datetime time
                        realArgValue = toGenericValue(arg.getArgType(), arg.getArgValue());
                        break;
                }
                varMap.put(arg.getArgName(), realArgValue);
            }
        }
        return varMap;
    }

    private Object toGenericValue(int argType, String argValue) {
        if (ArgType.get(argType) == null) {
            argType = ArgType.STRING.getValue();
        }
        try {
            switch (ArgType.get(argType)) {
                case BOOLEAN:
                    return Boolean.parseBoolean(argValue);
                case INTEGER:
                    return Integer.parseInt(argValue);
                case DOUBLE:
                    return Double.parseDouble(argValue);
                case LONG:
                    return Long.parseLong(argValue);
                default: //string date datetime time
                    return argValue;
            }
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
            return argValue;
        }
    }

    private List<Object> toListValue(Arg arg) {
        List<Object> list = new ArrayList<>();
        String[] values = arg.getArgValue().split(",,");
        for (String value : values) {
            list.add(toGenericValue(arg.getGenericType(), value));
        }
        return list;
    }

    private Map<String, Object> toMapValue(Arg arg) {
        Map<String, Object> map = new HashMap<>();
        String[] entries = arg.getArgValue().split(",,");
        for (String entry : entries) {
            String[] keyValueArray = entry.split("::");
            if (keyValueArray.length > 1) {
                map.put(keyValueArray[0], toGenericValue(arg.getGenericType(), keyValueArray[1]));
            }
        }
        return map;
    }
}