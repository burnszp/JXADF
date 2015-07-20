package com.jxtech.app.pubuser;

import com.jxtech.db.DataQuery;
import com.jxtech.i18n.JxLangResourcesUtil;
import com.jxtech.jbo.JboIFace;
import com.jxtech.jbo.JboSet;
import com.jxtech.jbo.JboSetIFace;
import com.jxtech.jbo.auth.JxSession;
import com.jxtech.jbo.util.DataQueryInfo;
import com.jxtech.jbo.util.JboUtil;
import com.jxtech.jbo.util.JxException;
import com.jxtech.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 康拓普用户信息- 健新科技优化实现
 *
 * @author wmzsoft@gmail.com
 * @date 2013.09
 */
public class PubUserSet extends JboSet implements PubUserSetIFace {
    private static final long serialVersionUID = -1228871314631022870L;
    private static final Logger LOG = LoggerFactory.getLogger(PubUserSet.class);

    @Override
    protected JboIFace getJboInstance() throws JxException {
        currentJbo = new PubUser(this);
        return currentJbo;
    }

    /**
     * @param userid 用户标识、登录帐号、邮箱、手机号
     * @return 返回加载的用户信息
     */
    @Override
    public JboIFace getUser(String userid) throws JxException {
        if (StrUtil.isNull(userid)) {
            LOG.warn("不知查询哪个用户，userid is null.");
            return null;
        }
        DataQueryInfo dq = getQueryInfo();
        dq.setWhereCause(" upper(user_id)=upper(?) or upper(login_id)=upper(?) or upper(email)=upper(?) or upper(mobile_number)=upper(?)");
        dq.setWhereParams(new Object[]{userid, userid, userid, userid});
        return super.getJboOfIndex(0, true);
    }

    /**
     * @param users 传入用户ＩＤ，多个之间用分号或逗号分隔。
     * @return 返回<user_id,name>
     */
    @Override
    public Map<String, String> getUsers(String users) throws JxException {
        if (StrUtil.isNull(users)) {
            return null;
        }
        String[] us = users.split(",");
        if (users.indexOf(";") > 0) {
            us = users.split(";");
        }
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < us.length; i++) {
            JboIFace ji = getUser(us[i]);
            if (ji != null) {
                map.put(ji.getString("user_id"), ji.getString("name"));
            }
        }
        return map;
    }

    /**
     * 获得同部门用户列表
     *
     * @param deptid
     * @return
     */
    @Override
    public List<JboIFace> getSameDepartmentUser(String deptid) throws JxException {
        if (StrUtil.isNullOfIgnoreCaseBlank(deptid)) {
            return null;
        }
        DataQueryInfo dq = getQueryInfo();
        dq.setWhereCause(" department_id=? ");
        dq.setWhereParams(new Object[]{deptid});
        return queryAll();
    }

    /**
     * 获得同部门用户列表
     *
     * @param deptid
     * @return Map<User_id,name>
     */
    @Override
    public Map<String, String> getSameDepartmentUsers(String deptid) throws JxException {
        List<JboIFace> list = getSameDepartmentUser(deptid);
        if (list == null) {
            return null;
        }
        int size = list.size();
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < size; i++) {
            JboIFace ji = list.get(i);
            map.put(ji.getString("user_id"), ji.getString("name"));
        }
        return map;
    }

    /**
     * 页面上面的重置密码功能
     *
     * @param param
     * @return
     * @throws JxException
     */
    public String resetPass(String param) throws JxException {
        String result = JxLangResourcesUtil.getString("app.pubuser.RESET_PASSWORD_OK");
        String[] params = param.split(",");
        String uid = params[0];
        String md5Pass = params[1];

        if (!StrUtil.isNull(uid)) {
            JboIFace user = JxSession.getMainApp().getJbo();
            if (null != user) {
                if (user.getString("USER_ID").equalsIgnoreCase(uid)) {
                    user.setString("PASSWORD", md5Pass);
                    user.getJboSet().commit();
                }
            }
        }

        return result;
    }

    /**
     * 查询所有用户
     *
     * @param active 是否激活
     * @return
     */
    public List<String> getAllUserList(boolean active) throws JxException {
        List<String> usersList = new ArrayList<String>();
        DataQueryInfo dataQueryInfo = new DataQueryInfo();
        StringBuilder querySb = new StringBuilder();
        querySb.append("USER_ID IS NOT NULL AND LOGIN_ID IS NOT NULL");
        if (active) {
            querySb.append(" AND ACTIVE = 1");
        }
        dataQueryInfo.setWhereCause(querySb.toString());

        dataQueryInfo.setWhereParams(new Object[]{});

        setQueryInfo(dataQueryInfo);

        List<JboIFace> users = queryAll();
        for (JboIFace jbo : users) {
            usersList.add(jbo.getString("USER_ID"));
        }

        return usersList;

    }

    @Override
    public List<JboIFace> query(String shipname) throws JxException {
        List<JboIFace> jboList = new ArrayList<JboIFace>();
        if (!StrUtil.isNull(shipname) && shipname.equalsIgnoreCase("PUB_ROLE_PUB_USER_ALL")) {
            JboIFace parent = getParent();
            if (null != parent) {
                return parent.getRelationJboSet("PUB_ROLE_PUB_USER_ALL").getJbolist();
            }
        } else {
            return super.query(shipname);
        }

        return jboList;
    }

    /**
     * 切换用户是否为某个角色成员
     *
     * @param params 1 表示添加到角色，0表示从角色移除
     * @return
     * @throws JxException
     */
    public String toggleUserInRole(String params) throws JxException {
        String result = "fail";
        String[] param = params.split(",");
        String action = param[0];
        String userId = param[1];

        JboIFace mainJbo = JxSession.getMainApp().getJbo();
        JboSetIFace jboSet = mainJbo.getRelationJboSet("PUB_ROLE_USERROLE_IDP");

        if ("1".equalsIgnoreCase(action)) {
            JboIFace jbo = jboSet.add();
            jbo.setObject("ROLE_ID", mainJbo.getObject("ROLE_ID"));
            jbo.setObject("USER_ID", userId);

        } else {
            List<JboIFace> allRoleUserList = jboSet.queryAll();
            for (JboIFace roleUser : allRoleUserList) {
                String roleUserId = roleUser.getString("USER_ID");
                String roleId = roleUser.getString("ROLE_ID");

                if (roleUserId.equalsIgnoreCase(userId) && roleId.equalsIgnoreCase(mainJbo.getString("ROLE_ID"))) {
                    roleUser.delete();
                }
            }
        }

        jboSet.commit();
        result = "ok";

        return result;

    }
}
