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
    LEDGER_ACCOUNT_BALANCE_NOT_ENOUGH(6005, "账户余额不足"),

    // Wardrobe errors (8xxx)
    WARDROBE_ITEM_NOT_FOUND(8001, "衣物不存在"),
    WARDROBE_ITEM_FORBIDDEN(8002, "无权限访问该衣物"),
    WARDROBE_OUTFIT_PLAN_NOT_FOUND(8003, "搭配方案不存在"),
    WARDROBE_NO_AVAILABLE_ITEMS(8004, "当前可用衣物较少，请先补充衣物"),
    OUTFIT_RENDER_TASK_NOT_FOUND(8005, "参考方案任务不存在"),
    OUTFIT_RENDER_PROFILE_INCOMPLETE(8006, "请先完善形象资料中的基础信息和三张资料照片"),

    // Mahjong helper errors (9xxx)
    MAHJONG_IMAGE_EMPTY(9001, "请先上传清晰的手牌照片"),
    MAHJONG_ANALYZE_FAILED(9002, "暂时无法给出胡牌提示，请稍后重试"),

    // Poker errors (9xxx)
    POKER_ROOM_NOT_FOUND(9101, "房间不存在"),
    POKER_ROOM_FULL(9102, "房间已满"),
    POKER_ROOM_STATUS_INVALID(9103, "房间状态不正确"),
    POKER_NOT_YOUR_TURN(9104, "未轮到你操作"),
    POKER_INVALID_ACTION(9105, "非法操作"),
    POKER_AMOUNT_INVALID(9106, "金额不合法"),
    POKER_NOT_IN_ROOM(9107, "不在该房间中"),
    POKER_SEAT_TAKEN(9108, "座位已被占用"),
    POKER_BUY_IN_INVALID(9109, "买入金额超出区间"),
    POKER_GAME_NOT_STARTED(9110, "游戏尚未开始"),

    // Resume errors (10xxx)
    RESUME_NOT_FOUND(10001, "简历不存在"),
    RESUME_PDF_FAILED(10002, "简历 PDF 生成失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
