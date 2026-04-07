package com.gamegroup.common.response;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Common errors
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或token已失效"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // Auth errors (1xxx)
    WX_LOGIN_FAILED(1001, "微信登录失败"),
    WX_CODE_INVALID(1002, "微信code无效"),
    TOKEN_INVALID(1003, "Token无效"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    USER_NOT_FOUND(1005, "用户不存在"),
    INVALID_PASSWORD(1006, "密码错误"),

    // Game errors (2xxx)
    GAME_NOT_FOUND(2001, "游戏不存在"),
    GAME_DISABLED(2002, "游戏已下线"),

    // Match errors (3xxx)
    VERIFY_CODE_INVALID(3001, "验证号无效"),
    ROOM_FULL(3002, "房间已满"),
    ROOM_EXPIRED(3003, "房间已过期"),
    ROOM_NOT_FOUND(3004, "房间不存在"),
    ALREADY_IN_ROOM(3005, "您已在该房间中"),
    ROOM_STATUS_INVALID(3006, "房间状态不正确"),

    // Auction errors (7xxx)
    AUCTION_NOT_FOUND(7001, "拍卖不存在"),
    AUCTION_STATUS_INVALID(7002, "拍卖状态不正确"),
    BID_AMOUNT_INVALID(7003, "出价金额不合法"),
    BID_ROUND_INVALID(7004, "出价轮次不合法"),
    BOX_NOT_FOUND(7005, "箱子不存在"),
    BID_ALREADY_SUBMITTED(7006, "本轮已提交出价"),
    AUCTION_NO_BIDS(7007, "本轮无人出价"),

    // Toilet errors (4xxx)
    TOILET_SESSION_NOT_FOUND(4001, "未找到进行中的计时会话"),
    TOILET_SESSION_ALREADY_RUNNING(4002, "已有进行中的计时会话"),

    // Jimeng / Volcengine Video (5xxx)
    JIMENG_NOT_CONFIGURED(5001, "即梦AI 未配置或未启用"),
    JIMENG_VIDEO_SUBMIT_FAILED(5002, "视频生成任务提交失败"),
    JIMENG_VIDEO_QUERY_FAILED(5003, "视频生成任务查询失败"),

    // Ledger errors (6xxx)
    LEDGER_ACCOUNT_NOT_FOUND(6001, "账户不存在"),
    LEDGER_CATEGORY_NOT_FOUND(6002, "分类不存在"),
    LEDGER_RECORD_NOT_FOUND(6003, "记录不存在"),
    LEDGER_TYPE_MISMATCH(6004, "记录类型与分类类型不一致"),
    LEDGER_ACCOUNT_BALANCE_NOT_ENOUGH(6005, "账户余额不足");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
