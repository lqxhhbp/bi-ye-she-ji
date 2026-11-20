package com.campus.express.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.express.common.enums.Role;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.common.util.PhoneMaskUtil;
import com.campus.express.dto.auth.ChangePasswordRequest;
import com.campus.express.dto.auth.LoginRequest;
import com.campus.express.dto.auth.RegisterRequest;
import com.campus.express.dto.auth.StationApplyRequest;
import com.campus.express.entity.Station;
import com.campus.express.entity.StationAudit;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.StationAuditMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.JwtUtil;
import com.campus.express.security.UserContext;
import com.campus.express.vo.auth.LoginVO;
import com.campus.express.vo.auth.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证与用户业务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final StationMapper stationMapper;
    private final StationAuditMapper stationAuditMapper;
    private final JwtUtil jwtUtil;

    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        SysUser existing = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, request.getPhone()));
        if (existing != null) {
            throw BusinessException.badRequest("该手机号已注册");
        }

        SysUser user = new SysUser();
        user.setPhone(request.getPhone());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setRealName(request.getRealName().trim());
        user.setRole(Role.STUDENT.getCode());
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        return toUserVO(user);
    }

    public LoginVO login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, request.getPhone()));
        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("手机号或密码错误");
        }
        if (user.getStatus() == 2) {
            throw BusinessException.forbidden("账号已禁用");
        }
        if (user.getStatus() != 1) {
            throw BusinessException.forbidden("账号尚未审核通过");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getStationId());
        return new LoginVO(token, toUserVO(user));
    }

    public UserVO currentUser() {
        SysUser user = userMapper.selectById(UserContext.requireUserId());
        if (user == null || user.getStatus() != 1) {
            throw BusinessException.unauthorized("用户不存在或已禁用");
        }
        return toUserVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        SysUser user = userMapper.selectById(UserContext.requireUserId());
        if (user == null || !BCrypt.checkpw(request.getOldPassword(), user.getPassword())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
        userMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyStation(StationApplyRequest request) {
        Station station = stationMapper.selectById(request.getStationId());
        if (station == null || station.getStatus() != 1) {
            throw BusinessException.notFound("站点不存在或已停用");
        }

        Long userId = UserContext.requireUserId();
        Long pendingCount = stationAuditMapper.selectCount(Wrappers.<StationAudit>lambdaQuery()
                .eq(StationAudit::getUserId, userId)
                .eq(StationAudit::getStatus, 0));
        if (pendingCount > 0) {
            throw BusinessException.badRequest("已有待审核的入驻申请");
        }

        StationAudit audit = new StationAudit();
        audit.setUserId(userId);
        audit.setStationId(request.getStationId());
        audit.setApplyReason(request.getApplyReason().trim());
        audit.setStatus(0);
        stationAuditMapper.insert(audit);
    }

    private UserVO toUserVO(SysUser user) {
        return UserVO.builder()
                .id(user.getId())
                .phone(PhoneMaskUtil.mask(user.getPhone()))
                .realName(user.getRealName())
                .role(user.getRole())
                .stationId(user.getStationId())
                .status(user.getStatus())
                .build();
    }
}
